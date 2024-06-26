import java.io.IOException;
import java.nio.channels.Channel;

import com.rabbitmq.client.*;

import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.lang.Process;
public class Init_Conn {

    private String[] UriList;
    private ConnectionFactory factory;
    private DefaultConsumer NodeHandler;
    private DefaultConsumer PlayerHandler;
    private com.rabbitmq.client.Channel NodechannelListen;
    private com.rabbitmq.client.Channel NodechannelPublish;
    private com.rabbitmq.client.Channel PlayerchannelListen;
    private com.rabbitmq.client.Channel PlayerchannelPublish;
    private int MaxPlayer;
    int NodeSize;


    public static void main(String[] args) throws IOException,TimeoutException{

        int NodeSize=10;
        Init_Conn Manager = new Init_Conn(NodeSize);
    }

    public Init_Conn(int t_NodeSize) throws IOException, TimeoutException
    {
        UriList = new String[4];
        NodeSize=t_NodeSize;
        MaxPlayer=1;

        UriList[0]="NodeA";
        UriList[1]="NodeB";
        UriList[2]="NodeC";
        UriList[3]="NodeD";

        factory = new ConnectionFactory();
        factory.setHost("LocalHost");
        System.out.println("Connecting ...");
        Connection connection= factory.newConnection();
        System.out.println("Connected !");

        //Listen a response of Node, if the coord send by Init_Conn is valid or not
        NodechannelListen = connection.createChannel();
        //send coord to a specific node, to check if this coord is valid
        NodechannelPublish = connection.createChannel();

        //listen if a new player want to connect (it's their first time here)
        PlayerchannelListen = connection.createChannel();

        //announce to a specific player his node, his coord and his real ID
        PlayerchannelPublish = connection.createChannel();


        //Creating all the channels
        NodechannelListen.queueDeclare("Initial_NL", false, false, false, null);
        NodechannelPublish.queueDeclare("Initial_NS", false, false, false, null);
        NodechannelPublish.exchangeDeclare("Initial_NS","direct");

        PlayerchannelListen.queueDeclare("Initial_PL", false, false, false, null);
        PlayerchannelPublish.queueDeclare("Initial_PS", false, false, false, null);
        PlayerchannelPublish.exchangeDeclare("Initial_PS","direct");

        try{
            //Clean old messages
            NodechannelListen.queuePurge("Initial_NL");
            NodechannelPublish.queuePurge("Initial_NS");
            PlayerchannelListen.queuePurge("Initial_PL");
            PlayerchannelPublish.queuePurge("Initial_PS");


        }catch(IOException e)
        {
            System.out.println("New Queues");
        }

        NodeHandler = new DefaultConsumer(NodechannelListen) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String message2 = new String(body, "UTF-8");
                System.out.println("New message !");

                NodeAnswer(message2);

            }
        };

        PlayerHandler = new DefaultConsumer(PlayerchannelListen) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String message2 = new String(body, "UTF-8");
                System.out.println("New message !");

                PlayerConnect(Long.parseLong(message2));
            }
        };
        System.out.println("Listening ...");
        NodechannelListen.basicConsume("Initial_NL", true, NodeHandler);
        PlayerchannelListen.basicConsume("Initial_PL", true, PlayerHandler);

    }
    //method used only for players to connect (called once for each player)
    public void PlayerConnect(long tmpID)
    {
        int id=-1;
        try
        {
            //it's always incrementing
            id=MaxPlayer++;

            //try to find legal coord (not in conflict with another player (first try))
            NodechannelPublish.basicPublish("Initial_NS",getRandomNode()+"CL",null,(getRandomLoc(id)+" "+tmpID+" Initial_NL").getBytes());
        }catch (IOException e)
        {
            System.out.println(e);
        }
    }

    public void NodeAnswer(String message) throws IOException
    {
        String[] output=message.split(" ");
        if(output[1].compareTo("SUCCESS")!=0)
        {
            System.out.println("Failed, finding new coord");
            //try another coord (because the last one is in conflict with a player)
            NodechannelPublish.basicPublish("Initial_NS",output[0]+"CL",null,(getRandomLoc(Integer.parseInt(output[2]))+" "+output[4]+" Initial_NL").getBytes());
        }
        else
        {
            System.out.println("Success, player has coord");
            //send to the new player with which node he's connected
            System.out.println( message+"|"+output[2]);
            PlayerchannelPublish.basicPublish("Initial_PS",output[3],null,("NODE "+output[0]+" "+output[2]).getBytes());
        }
    }

    public String getNodename(int id)
    {
        return UriList[id];
    }

    public String getRandomLoc(int id)
    {
        //randomize coordinates
        Random rand=new Random();
        int x = rand.nextInt(0,NodeSize);
        int y = rand.nextInt(0,NodeSize);
        return Integer.toString(id)+" STILL "+Integer.toString(x)+" "+Integer.toString(y);
    }
    public String getRandomNode()
    {
        //randomize node
        Random rand=new Random();
        int randomNum = rand.nextInt(0,3);
        System.out.println("rand ID Serveur: "+randomNum);
        return getNodename(randomNum);
    }
}
