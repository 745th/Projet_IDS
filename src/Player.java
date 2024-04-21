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

    Point MyLoc;

    public static void main(String[] args) throws IOException,TimeoutException{
        Player player = new Player();
        player.ConnectToNetwork();
    }

    public Player() throws IOException,TimeoutException
    {
        plat = new Plateau(10, 10);
        try {
            jeu = new FenetreJeu(plat,this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Loc = new Hashtable<>();
        Nodename="";
    }

    public void ConnectToNetwork() throws IOException, TimeoutException
    {
        long tid = new Date().getTime();

        //first time connecting to a node
        ID=Long.toString(tid);
        OldID=ID;
        factory = new ConnectionFactory();
        System.out.println(Nodename);
        factory.setHost("LocalHost");

        System.out.println("Connecting ...");

        connection = factory.newConnection();

        //send to a node or InitConn an action
        channelPublish = connection.createChannel();

        //listen for confirmation or new event (like new player or movement of players)
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
                    String[] output = message2.split(" ");
                    ID=output[2];
                    ConnectToANode(output[1]);
                    System.out.println("First node :" + output[1]);
                    channelListen.queueUnbind(OldID, "Initial_PS", OldID);
                    channelListen.queueDelete(OldID);
                    //connect to a node and get his new ID
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
        System.out.println("Switching node");
        if(Nodename.compareTo("")!=0)
        {
            //switching to a new node
            channelPublish.basicPublish("", Nodename+"L", null, (ID+" DISCONNECT").getBytes());
            channelListen.queueUnbind(ID, Nodename+"S", "");
            Loc.clear();
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
                        Work(consumerTag, envelope.getDeliveryTag(), message2);
                    } catch (TimeoutException e) {
                        System.out.println(e);
                    }
                }
            };
            channelListen.basicConsume(ID, true, Handler);
        }
        Nodename=t_Nodename;
        jeu.updateBackground();

        channelListen.queueBind(ID, Nodename+"S", ID);

        channelPublish.basicPublish("", Nodename+"L", null, (ID+" CONNECT Player").getBytes());
    }

    public void Work(String consumer,long deliveryTag, String message) throws IOException, TimeoutException {
        //receive movements of other players
        String[] output=message.split(" ");
        int ID_in = Integer.parseInt(output[0]);
        Point loc = new Point(Integer.parseInt(output[1]),Integer.parseInt(output[2]));
        System.out.println(ID_in+" Loc: "+loc.toString());

        //Receive Events ...
        switch(output[3])
        {
            case "CONNECT":
                //a node send a CONNECT for players needing all occupied coordinate
                if (!Loc.containsKey(loc)) {
                    Loc.put(ID_in, loc);
                }
                break;
            case "UPDATE":
                //a node send a UPDATE if a player has move successufully
                if (loc.x != -1) {
                    //add or replace coord of a player
                    if (Loc.containsKey(loc)) {
                        Loc.replace(ID_in, loc);
                    } else {
                        Loc.put(ID_in, loc);
                    }
                } else {
                    //if a player disconnect, it's removing from Loc (indicate by x=-1 and y=-1)
                    Loc.remove(ID_in);
                }
                if(ID_in == Integer.parseInt(ID))
                    MyLoc=loc;

                //The coucou is always send firstly by checking if the static player watch a player near him
                if(isCoucou(loc))
                    channelPublish.basicPublish("", Nodename + "L", null, (ID + " COUCOU "+Integer.toString(ID_in)+" 0").getBytes());
                break;
            case "SWITCH":
                //change to another node
                ConnectToANode(output[4]);
                break;
            case "COUCOU":
                //for not looping coucou we're checking if it's a response, indicate by 1 at the end
                if(Integer.parseInt(output[1])==0)
                    channelPublish.basicPublish("", Nodename + "L", null, (ID + " COUCOU "+output[4]+" 1").getBytes());
                System.out.println("----------- Coucou de "+output[4]+" ! -----------");
                break;
        }

        //Updating JFRAME
        plat.joueur = Integer.parseInt(ID);
        plat.clean();
        Loc.forEach((K,v)->plat.plateau[v.x][v.y] = K);
        jeu.plateauGraphique.repaint();


    }

    public void Disconnect()
    {
        try {
            //Announce to his node he's gone and clean his channel
            //called only by closing the window
            channelPublish.basicPublish("", Nodename + "L", null, (ID + " DISCONNECT").getBytes());
            channelListen.queuePurge(ID);
            channelListen.queueDelete(ID);
        }catch(IOException e)
        {
            System.out.println(e);
        }
    }

    public void Move(PlayerAction d)
    {
        try {
            //method used in the keyhandler to move the player
            channelPublish.basicPublish("", Nodename+"L", null, (ID+" "+d.name()).getBytes());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public boolean isCoucou(Point loc)
    {

        //check if the moving player is near
        if((MyLoc.x - loc.x ==1 || MyLoc.x - loc.x ==-1) && MyLoc.y - loc.y==0)
            return true;
        if((MyLoc.y - loc.y ==1 || MyLoc.y - loc.y ==-1) && MyLoc.x - loc.x==0)
            return true;

        return false;
    }
}
