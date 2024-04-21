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
    private Channel channelNodeComL;
    private Channel channelNodeComS;
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
        channelNodeComL = connection.createChannel();
        channelNodeComS = connection.createChannel();


        channelListen.queueDeclare(Nodename+"L", false, false, false, null);

        channelConnexionL.queueDeclare(Nodename+"CL", false, false, false, null);
        channelConnexionS.queueDeclare("Initial_NL", false, false, false, null);
        channelNodeComS.queueDeclare(Nodename+"NS", false, false, false, null);

        //channelConnexionL.exchangeDeclare("Initial_PS", "direct");
        channelConnexionL.queueBind(Nodename+"CL", "Initial_NS", Nodename+"CL");
        channelConnexionL.exchangeDeclare("NodeCom", "direct");
        for(int i=1;i<=4;i++)
        {
            //we use the same channel for NodeCommunication and InitComm
            channelConnexionL.exchangeDeclare("Node" + Integer.toString(i) + "NS", "direct");
            if(Character.getNumericValue(Nodename.charAt(Nodename.length()-1))!=i) {
                channelConnexionL.queueBind(Nodename + "CL", "Node" + Integer.toString(i) + "NS", Nodename);
            }
        }

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

                    switch (message2.split(" ")[1])
                    {
                        case "CONNECT":
                            ConnectPlayer(message2);
                            break;

                        case "DISCONNECT":
                            DisconnectPlayer(message2);
                            break;
                        default:
                            MovePlayer(message2);
                            break;
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
        if(MovePlayer(message))
        {
            System.out.println("New Player in coming");
            if(output.length==5 && output[4].compareTo("Player")==0)
            {

            }
            else
            {
                channelConnexionS.basicPublish("", "Initial_NL",null,(Nodename+" SUCCESS "+output[0]+" "+output[4]).getBytes());
            }

        }
        else
        {
            System.out.println("Coord is occupy");
            if(output.length==5 && output[4].compareTo("Player")==0)
            {
                String [] player_list=Players.keySet().toArray(new String[Players.size()]);
                for (String i : player_list)
                {
                    channelPublish.basicPublish(Nodename + "S", output[0], null, (output[0] + " " + Integer.toString(Players.get(i).x) + " " + Integer.toString(Players.get(i).y)).getBytes());
                }
            }
            else
            {
                channelConnexionS.basicPublish("", "Initial_NL",null, (Nodename+" "+output[0]+" "+output[4]).getBytes());
            }

        }

    }

    private void DisconnectPlayer(String message) throws IOException, TimeoutException
    {
        System.out.println("Disconnect a player");
        String[] output=message.split(" ");
        int x=Players.get(output[0]).x;
        int y=Players.get(output[0]).y;
        Players.remove(output[0]);
        channelPublish.basicPublish(Nodename+"S", "",null, (output[0]+" -1 -1").getBytes());
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
        System.out.println("try: "+Integer.toString(Grid[x][y])+" "+Integer.toString(x)+" "+Integer.toString(y));
        if(SwitchNode(output[0],x,y)) {
            if (Grid[x][y] == -1) {
                Grid[x][y] = Integer.parseInt(output[0]);
                if (Players.get(output[0]).x != -1)
                    Grid[Players.get(output[0]).x][Players.get(output[0]).y] = -1;
                Players.replace(output[0], new Point(x, y));
                channelPublish.basicPublish(Nodename + "S", "", null, (output[0] + " " + Integer.toString(x) + " " + Integer.toString(y)).getBytes());
                return true;
            } else {
                //can't move here
                Players.remove(output[0]);
                return false;
            }
        }
        return true;
    }

    private boolean SwitchNode(String PlayerId,int x,int y) throws IOException
    {
        //where does the player has to go by going out the limit of the node
        //our layout look like that:
        /*
            NodeA | NodeB
            -------------
            NodeC | NodeD
        */
        String newNode=""; //it's impposible to not have a Node outside of this list
        Point newCoord = new Point(x,y);
        switch(Nodename)
        {
            case "NodeA":
                if(newCoord.x<0 || newCoord.y<0)
                {
                    System.out.println("Nothing is here");
                }else if(newCoord.x>=Size)
                {
                    newNode="NodeB";
                    newCoord.x=0;
                }
                else if(newCoord.y>=Size)
                {
                    newCoord.y=0;
                    newNode="NodeC";
                }
                break;
            case "NodeB":
                if(newCoord.x>=Size || newCoord.y<0)
                {
                    System.out.println("Nothing is here");
                }else if(newCoord.x<0)
                {
                    newCoord.x=Size-1;
                    newNode="NodeA";
                }
                else if(newCoord.y>=Size)
                {
                    newCoord.y=0;
                    newNode="NodeD";
                }
                break;
            case "NodeC":
                if(newCoord.x<0 || newCoord.y>=Size)
                {
                    System.out.println("Nothing is here");
                }else if(newCoord.x>=Size)
                {
                    newCoord.x=0;
                    newNode="NodeD";
                }
                else if(newCoord.y<0)
                {
                    newCoord.y=Size-1;
                    newNode="NodeA";
                }
                break;
            case "NodeD":
                if(newCoord.x>=Size || newCoord.y>=Size)
                {
                    System.out.println("Nothing is here");
                }else if(newCoord.x<0)
                {
                    newCoord.x=Size-1;
                    newNode="NodeC";
                }
                else if(newCoord.y<0)
                {
                    newCoord.y=Size-1;
                    newNode="NodeB";
                }
                break;
        }
        if(newNode.compareTo("")!=0) {
            channelConnexionL.basicPublish(Nodename + "NS", newNode, null, (PlayerId + " STILL " + Integer.toString(newCoord.x) + " " + Integer.toString(newCoord.y)).getBytes());
            return false;
        }
        return true;
    }

}
