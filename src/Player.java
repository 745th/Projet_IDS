import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeoutException;
public class Player {


    private ConnectionFactory factory;
    Channel channelPublish;
    Channel channelListen;
    private DefaultConsumer Handler;

    String ID;
    int[] Loc;
    String Nodename;
    Connection connection;

    public static void main(String[] args) throws IOException,TimeoutException{
        Player player = new Player();

    }
    public Player() throws IOException,TimeoutException
    {
        Loc = new int[2];
        Loc[0]=0;
        Loc[1]=0;

        Init_Conn conn = new Init_Conn();
        Nodename="";
        Connect(conn.getNodename(1));

    }

    public void Connect(String t_Nodename) throws IOException, TimeoutException
    {
        long tid = new Date().getTime();
        //changing sector
        if(Nodename.compareTo("")!=0)
        {
            channelListen.queueUnbind(ID, Nodename+"S", "");
        }
        else
        {
            ID=Long.toString(tid);
            factory = new ConnectionFactory();
            System.out.println(Nodename);
            factory.setHost("LocalHost");

            System.out.println("Connecting ...");

            connection = factory.newConnection();
            channelPublish = connection.createChannel();
            channelListen = connection.createChannel();
            channelListen.queueDeclare(ID, false, false, false, null);
            channelListen.basicQos(1);
            System.out.println("Handler");

            Handler = new DefaultConsumer(channelListen) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties,
                                           byte[] body) throws IOException {
                    String message2 = new String(body, "UTF-8");
                    System.out.println("New message !");
                    try {
                        System.out.println("CT :" + consumerTag);
                        Work(consumerTag, envelope.getDeliveryTag(), message2);
                    } catch (TimeoutException e) {
                        System.out.println(e);
                    }
                }
            };
            channelListen.basicConsume(ID, false, Handler);
        }
        Nodename=t_Nodename;

        channelListen.exchangeDeclare(Nodename+"S", "fanout");
        //channelPublish.queueDeclare(Nodename+"L", false, false, false, null);
        channelListen.queueBind(ID, Nodename+"S", "");

        channelPublish.basicPublish("", Nodename, null, (ID+" CONNECT").getBytes());
    }

    public void Work(String consumer,long deliveryTag, String message) throws IOException, TimeoutException {
        System.out.println("work");
    }

    public void Move(PlayerAction d) throws IOException
    {
        channelPublish.basicPublish("", Nodename+"L", null, (ID+" "+d.name()).getBytes());
    }
}
