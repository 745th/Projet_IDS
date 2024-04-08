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
    private Channel channelPublish;
    private ArrayList<Channel> PlayerChannel;
    private DefaultConsumer Handler;
    private String Nodename;
    public Node(int t_Size)
    {
        Size=t_Size;
        Players= new Hashtable<>();
        PlayerChannel = new ArrayList<>();
        Grid = new int[Size][Size];
        for(int i =0;i<Size;i++) {
            for (int j = 0; j < Size; j++) {
                Grid[i][j] = -1;
            }
        }
    }
    public static void main(String[] args) throws IOException,TimeoutException{
        Node node = new Node(10);

    }
    public void Init(String t_Nodename) throws IOException, TimeoutException
    {
        factory = new ConnectionFactory();
        System.out.println(Nodename);
        factory.setHost("LocalHost");
        Nodename=t_Nodename;
        System.out.println("Connecting ...");

        Connection connection = factory.newConnection();
        System.out.println("Handler");
        channelListen = connection.createChannel();
        channelPublish = connection.createChannel();
        channelListen.queueDeclare(Nodename+"L", false, false, false, null);
        channelPublish.queueDeclare(Nodename+"S", false, false, false, null);
        channelPublish.exchangeDeclare(Nodename+"S", "fanout");
        channelListen.basicQos(1);
        Handler = new DefaultConsumer(channelListen) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String message2 = new String(body, "UTF-8");
                System.out.println("New message !");
                try {
                    System.out.println("CT :" + consumerTag);
                    Work(message2,null);
                } catch (TimeoutException e) {
                    System.out.println(e);
                }
            }
        };
        channelListen.basicConsume(Nodename+"L", false, Handler);
    }

    public boolean Work(String message,Point coord) throws IOException, TimeoutException {
        System.out.println("work");
        String[] output=message.split(" ");
        int y,x;
        if(coord!=null)
        {
            x= coord.x;
            y=coord.y;
        }
        else
        {
            y=Players.get(output[0]).y;
            x=Players.get(output[0]).x;
        }

        PlayerAction e =PlayerAction.valueOf(output[1]);
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

        if(Grid[x][y]==-1)
        {
            Grid[x][y]=Integer.parseInt(output[0]);
            Grid[Players.get(output[0]).x][Players.get(output[0]).y]=-1;
            for(int i=0;i<Players.size();i++)
            {
                channelPublish.basicPublish(Nodename+"S", null,null, (output[0]+" "+Integer.toString(x)+" "+Integer.toString(y)).getBytes());
            }
            return true;
        }
        else
        {
            //can't move here
            //channelPublish.basicPublish("", output[0],null, ("0").getBytes());
            return false;
        }
    }
}
