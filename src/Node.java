import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.*;
import java.awt.Point;
import java.util.concurrent.TimeoutException;

public class Node {

    int Size;
    int Grid[][];
    private ConnectionFactory factory;

    private Map<String,Point> Players;

    private Channel channelListen;
    private Channel channelConnexionL;
    private Channel channelConnexionS;
    private Channel channelPublish;
    private DefaultConsumer Handler;
    private DefaultConsumer Handler2;
    private String Nodename;
    public Node(String name,int t_Size)
    {
        Size=t_Size;
        Nodename=name;
        Players= new Hashtable<>();
        Grid = new int[Size][Size];
        for(int i =0;i<Size;i++) {
            for (int j = 0; j < Size; j++) {
                Grid[i][j] = -1;
            }
        }
    }
    public static void main(String[] args) throws IOException,TimeoutException{
        Node node = new Node(args[0],Integer.parseInt(args[1]));
        node.Init(args[0]);
    }
    public void Init(String t_Nodename) throws IOException, TimeoutException
    {
        factory = new ConnectionFactory();
        System.out.println(Nodename);
        factory.setHost("LocalHost");
        Nodename=t_Nodename;
        System.out.println("Connecting ...");

        Connection connection = factory.newConnection();
        System.out.println("Connected !");
        channelListen = connection.createChannel();
        channelPublish = connection.createChannel();
        channelConnexionL = connection.createChannel();
        channelConnexionS = connection.createChannel();



        channelListen.queueDeclare(Nodename+"L", false, false, false, null);

        channelConnexionL.queueDeclare(Nodename+"CL", false, false, false, null);
        channelConnexionS.queueDeclare("Initial_NL", false, false, false, null);

        channelConnexionL.exchangeDeclare("Initial_PS", "direct");
        channelConnexionL.queueBind(Nodename+"CL", "Initial_NS", Nodename+"CL");

        channelPublish.queueDeclare(Nodename+"S", false, false, false, null);

        channelPublish.exchangeDeclare(Nodename+"S", "fanout");
        channelListen.basicQos(1);

        try{
            channelConnexionL.queuePurge(Nodename+"CL");
            channelConnexionS.queuePurge("Initial_NL");
            channelPublish.queuePurge(Nodename+"S");
            channelListen.queuePurge(Nodename+"L");

        }catch(IOException e)
        {
            System.out.println("New Queues");
        }

        Handler = new DefaultConsumer(channelListen) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String message2 = new String(body, "UTF-8");
                System.out.println("New message !");
                try {
                    System.out.println("CT :" + consumerTag);
                    if(message2.split(" ")[1].compareTo("CONNECT")==0)
                    {
                        //send coords of players
                    }
                    else {
                        MovePlayer(message2);
                    }
                } catch (TimeoutException e) {
                    System.out.println(e);
                }
            }
        };

        Handler2 = new DefaultConsumer(channelConnexionL) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String message2 = new String(body, "UTF-8");
                System.out.println("New message !");
                try {
                    System.out.println("CT :" + consumerTag);
                    ConnectPlayer(message2);
                } catch (TimeoutException e) {
                    System.out.println(e);
                }
            }
        };

        channelListen.basicConsume(Nodename+"L", true, Handler);
        channelConnexionL.basicConsume(Nodename+"CL", true, Handler2);
    }

    public void ConnectPlayer(String message) throws IOException, TimeoutException
    {
        String[] output=message.split(" ");
        System.out.println(message);
        if(MovePlayer(message))
        {
            System.out.println("New Player in coming");
            channelConnexionS.basicPublish("", "Initial_NL",null,(Nodename+" SUCCESS "+output[0]).getBytes());
        }
        else
        {
            System.out.println("Coord is occupy");
            channelConnexionS.basicPublish("", "Initial_NL",null, (Nodename+" "+output[0]).getBytes());
        }

    }

    public boolean MovePlayer(String message) throws IOException, TimeoutException {
        System.out.println("Move a player");
        String[] output=message.split(" ");
        int y,x;
        if(Players.containsKey(output[0])) {
            y = Players.get(output[0]).y;
            x = Players.get(output[0]).x;
        }
        else
        {
            Players.put(output[0],new Point(-1,-1));
            x=Integer.parseInt(output[2]);
            y=Integer.parseInt(output[3]);
        }
        PlayerAction e=PlayerAction.STILL;
        try {
            e = PlayerAction.valueOf(output[1]);
        }catch(IllegalArgumentException ee){}
        switch(e)
        {
            case UP:
                --y;
                break;
            case LEFT:
                --x;
                break;
            case RIGHT:
                ++x;
                break;
            case DOWN:
                y++;
                break;
        }
        System.out.println(":: "+Integer.toString(Grid[x][y])+" "+Integer.toString(x)+" "+Integer.toString(y));
        if(Grid[x][y]==-1)
        {
            Grid[x][y]=Integer.parseInt(output[0]);
            if(Players.get(output[0]).x!=-1)
                Grid[Players.get(output[0]).x][Players.get(output[0]).y]=-1;
            Players.replace(output[0],new Point(x,y));
            //for(int i=0;i<Players.size();i++)
            //{
                channelPublish.basicPublish(Nodename+"S", "",null, (output[0]+" "+Integer.toString(x)+" "+Integer.toString(y)).getBytes());
            //}
            return true;
        }
        else
        {
            //can't move here
            //channelPublish.basicPublish("", output[0],null, ("0").getBytes());
            Players.remove(output[0]);
            return false;
        }
    }
}
