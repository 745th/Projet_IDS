import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.Date;
import java.awt.Point;
import java.util.Hashtable;
import java.util.concurrent.TimeoutException;
import java.util.Map;


public class Player {


    private ConnectionFactory factory;
    com.rabbitmq.client.Channel channelPublish;
    com.rabbitmq.client.Channel channelListen;
    private DefaultConsumer Handler;

    String ID;
    String OldID;
    Plateau plat;
    FenetreJeu jeu;
    Map<Integer,Point>Loc;
    String Nodename;
    Connection connection;

    public static void main(String[] args) throws IOException,TimeoutException{
        Player player = new Player();
        player.ConnectToNetwork();
    }

    public Player() throws IOException,TimeoutException
    {
        plat = new Plateau(10, 10);
        try {
            jeu = new FenetreJeu(plat);
            // FenetreGraphique mainFen = new FenetreGraphique(p);
            jeu.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Loc = new Hashtable<>();
        Nodename="";
    }

    public void ConnectToNetwork() throws IOException, TimeoutException
    {
        long tid = new Date().getTime();
        //changing sector
            //first time connecting to a node
        ID=Long.toString(tid);
        OldID=ID;
        factory = new ConnectionFactory();
        System.out.println(Nodename);
        factory.setHost("LocalHost");

        System.out.println("Connecting ...");

        connection = factory.newConnection();
        channelPublish = connection.createChannel();
        channelListen = connection.createChannel();
        channelListen.queueDeclare(ID, false, false, false, null); // reçois 2 message: son ID puis son NODE
        channelListen.basicQos(1);
        System.out.println("Connected !");

        Handler = new DefaultConsumer(channelListen) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String message2 = new String(body, "UTF-8");
                System.out.println("New message !");
                try {
                    System.out.println("CT :" + consumerTag);
                    //double queuebind
                    String[] output = message2.split(" ");
                    ID=output[2];
                    ConnectToANode(output[1]);
                    System.out.println("First node :" + output[1]);
                    channelListen.queueUnbind(OldID, "Initial_PS", OldID);
                    channelListen.queueDelete(OldID);
                } catch (TimeoutException e) {
                    System.out.println(e);
                }
            }
        };
        channelListen.basicConsume(ID, true, Handler);

        channelListen.exchangeDeclare("Initial_PS", "direct");
        //channelPublish.queueDeclare(Nodename+"L", false, false, false, null);
        channelListen.queueBind(ID, "Initial_PS", OldID);

        channelPublish.basicPublish("", "Initial_PL", null, Long.toString(tid).getBytes());
    }
    public void ConnectToANode(String t_Nodename) throws IOException, TimeoutException
    {
        //changing sector
        if(Nodename.compareTo("")!=0)
        {
            //switching to a new node
            channelPublish.basicPublish("", Nodename+"L", null, (ID+" DISCONNECT").getBytes());
            channelListen.queueUnbind(ID, Nodename+"S", "");
        }
        else
        {
            //first time connecting to a node
            channelListen.queueDeclare(ID, false, false, false, null);
            channelListen.basicQos(1);
            System.out.println("Connecting to a new node");

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
            channelListen.basicConsume(ID, true, Handler);
        }
        Nodename=t_Nodename;

        channelListen.exchangeDeclare(Nodename+"S", "fanout");
        //channelPublish.queueDeclare(Nodename+"L", false, false, false, null);
        channelListen.queueBind(ID, Nodename+"S", "");

        channelPublish.basicPublish("", Nodename+"L", null, (ID+" CONNECT Player").getBytes());
    }

    public void Work(String consumer,long deliveryTag, String message) throws IOException, TimeoutException {
        //receive movements of other players
        String[] output=message.split(" ");
        int ID = Integer.parseInt(output[0]);
        Point loc = new Point(Integer.parseInt(output[1]),Integer.parseInt(output[2]));
        System.out.println(ID+" Loc: "+loc.toString());
        if(output.length==4 && output[3].compareTo("CONNECT")==0)
        {
            if (!Loc.containsKey(loc)) {
                Loc.put(ID, loc);
            }
        }
        else {
            if (loc.x != -1) {

                if (Loc.containsKey(loc)) {
                    Loc.replace(ID, loc);
                } else {
                    Loc.put(ID, loc);
                }
            } else {
                Loc.remove(ID);
            }
        }

        //APPEL DE RAPH
        Loc.forEach((K,v)->plat.plateau[v.x][v.y] = K);
        jeu.plateauGraphique.repaint();


    }

    public void Move(PlayerAction d) throws IOException
    {
        channelPublish.basicPublish("", Nodename+"L", null, (ID+" "+d.name()).getBytes());
    }
}
