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
        //a node is always a square Size x Size
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

        //Listen for a action to do in this node
        channelListen = connection.createChannel();

        //publish if a change occure in this node to players
        channelPublish = connection.createChannel();

        //listen for new player coming from another node or from Init_Conn
        channelConnexionL = connection.createChannel();

        //Answer back if the player can connect or not
        channelConnexionS = connection.createChannel();

        //ask to a node to exchange a player with another node
        channelNodeComS = connection.createChannel();


        channelListen.queueDeclare(Nodename+"L", false, false, false, null);

        channelConnexionL.queueDeclare(Nodename+"CL", false, false, false, null);
        channelConnexionS.queueDeclare("", false, false, false, null);
        channelNodeComS.queueDeclare(Nodename+"NS", false, false, false, null);

        //bind with Init_Conn
        channelConnexionL.queueBind(Nodename+"CL", "Initial_NS", Nodename+"CL");
        for(int i=0;i<=3;i++)
        {
            //we use the same channel for NodeCommunication and InitComm for listening
            char l = 'A';
            l+=i;

            //bind with all node
            channelConnexionL.exchangeDeclare("Node" + String.valueOf(l) + "NS", "direct");
            if(Nodename.charAt(Nodename.length()-1)!='A'+i) {
                channelConnexionL.queueBind(Nodename + "CL", "Node" + String.valueOf(l) + "NS", Nodename);
            }
        }
        channelPublish.queueDeclare(Nodename+"S", false, false, false, null);

        channelPublish.exchangeDeclare(Nodename+"S", "direct");
        channelListen.basicQos(1);

        try{
            //clean old message
            channelConnexionL.queuePurge(Nodename+"CL");
            channelConnexionS.queuePurge("Initial_NL");
            channelPublish.queuePurge(Nodename+"S");
            channelListen.queuePurge(Nodename+"L");
            channelNodeComS.queuePurge(Nodename+"NS");

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

                //Listen to do something in this node ...
                try {
                    System.out.println("CT :" + consumerTag);

                    switch (message2.split(" ")[1])
                    {
                        case "CONNECT":
                            //A new player is connected, it's a message send only by player to get all players coordinate
                            ConnectPlayer(message2);
                            break;

                        case "DISCONNECT":
                            //A player want to disconect from this node, it's call only by players if he goes to another node, or if the JFRAME is closing
                            DisconnectPlayer(message2);
                            break;
                        case "SUCCESS":
                            //After sending a message to a node to send a player, the node respond back positively so this node alert the player
                            SwitchPlayer(message2);
                            break;
                        case "FAILED":
                            //does nothing, it's occur only if a node respond back negatively after this node asking if a player can go to the other node
                            System.out.println("Switch Failed");
                            break;
                        case "COUCOU":
                            //a player has seen another one ! This node transfer this message to the target player
                            Coucou(message2);
                            break;
                        default:
                            //try to move a player
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

    public void Coucou(String message) throws IOException
    {
        String[] output=message.split(" ");
        //send a coucour to the target player
        //the other one will respond back only if this message is the first one (like ping pong)
        channelPublish.basicPublish(Nodename + "S", output[2],null,("0 "+output[3]+" 0 COUCOU "+output[0]).getBytes());
    }
    public void SwitchPlayer(String message) throws IOException
    {
        String[] output=message.split(" ");
        //alert the player, that he can change node
        channelPublish.basicPublish(Nodename + "S", output[2],null,("0 0 0 SWITCH "+output[0]).getBytes());
    }

    public void ConnectPlayer(String message) throws IOException, TimeoutException
    {
        String[] output=message.split(" ");
        if(output[2].compareTo("Player") == 0) {
            //send coord of players location to output[0] (a player asking for coords)
            System.out.println("Send Coord of Players "+output[0]);
            String[] player_list = Players.keySet().toArray(new String[Players.size()]);
                for (String i : player_list) {
                    channelPublish.basicPublish(Nodename + "S", output[0], null, (i + " " + Integer.toString(Players.get(i).x) + " " + Integer.toString(Players.get(i).y)+" UPDATE").getBytes());
                }

        }
        else {
            if (MovePlayer(message)) {
                //InitConn or a node succeed to get a valid coord
                System.out.println("New Player in coming");
                if (output.length == 6)
                    channelConnexionS.basicPublish("", output[5], null, (Nodename + " SUCCESS " + output[0] + " " + output[4]).getBytes());
                //else it's a player trying to move
            } else {

                System.out.println("Coord is occupy");
                if (output.length == 6) {
                    //InitConn or a node send coords being occupied
                    //alert InitConn or the node in output[0]
                    channelConnexionS.basicPublish("", output[5], null, (Nodename + " FAILED " + output[0] + " " + output[4]).getBytes());

                }
                //else it's a player trying to move

            }
        }

    }

    private void DisconnectPlayer(String message) throws IOException, TimeoutException
    {
        //remove from known player of this node
        System.out.println("Disconnect a player");
        String[] output=message.split(" ");
        System.out.println(message);
        int x=Players.get(output[0]).x;
        int y=Players.get(output[0]).y;
        Players.remove(output[0]);
        Grid[x][y]=-1;
        //alert everyone a player is no more with them :(
        for(String p : Players.keySet())
            channelPublish.basicPublish(Nodename+"S", p,null, (output[0]+" -1 -1 UPDATE").getBytes());
    }

    public boolean MovePlayer(String message) throws IOException, TimeoutException {
        System.out.println("Move a player : "+message);
        String[] output=message.split(" ");
        int y,x;
        if(Players.containsKey(output[0])) {
            //get the coord is the node know this player
            y = Players.get(output[0]).y;
            x = Players.get(output[0]).x;
        }
        else
        {
            //get the coord from message if the player is new from this node
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
        //check if the current coords are outside the node but in sector of another node
        if(SwitchNode(output[0],x,y) ) {
            //check now if it's outside
            if(x>=0 && x<Size && y>=0 && y<Size) {
                System.out.println("try: " + Integer.toString(Grid[x][y]) + " " + Integer.toString(x) + " " + Integer.toString(y));

                //if the cell is free
                if (Grid[x][y] == -1) {
                    Grid[x][y] = Integer.parseInt(output[0]);
                    if (!Players.containsKey(output[0])) {
                        //add player if he doesn't exist here
                        Players.put(output[0], new Point(-1, -1));
                    } else {
                        //if it's not the first time the player came, then his old coord will be free
                        if (Players.get(output[0]).x != -1)
                            Grid[Players.get(output[0]).x][Players.get(output[0]).y] = -1;
                    }
                    Players.replace(output[0], new Point(x, y));

                    //alert everyone in this node of a player has moved
                    for (String p : Players.keySet())
                        channelPublish.basicPublish(Nodename + "S", p, null, (output[0] + " " + Integer.toString(x) + " " + Integer.toString(y) + " UPDATE").getBytes());
                    return true;
                } else {
                    //can't move here
                    return false;
                }
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
        String newNode=""; //it's impossible to not have a Node outside of this list
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
                    System.out.println("switch "+newNode);
                }
                else if(newCoord.y>=Size)
                {
                    newCoord.y=0;
                    newNode="NodeC";
                    System.out.println("switch "+newNode);
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
                    System.out.println("switch "+newNode);
                }
                else if(newCoord.y>=Size)
                {
                    newCoord.y=0;
                    newNode="NodeD";
                    System.out.println("switch "+newNode);
                }
                break;
            case "NodeC":
                if(newCoord.x<0 || newCoord.y>=Size)
                {
                    System.out.println("Nothing is here");
                }else if(newCoord.x>=Size)
                {
                    newCoord.x=0;
                    System.out.println("switch D");
                    newNode="NodeD";
                    System.out.println("switch "+newNode);
                }
                else if(newCoord.y<0)
                {
                    System.out.println("switch A");
                    newCoord.y=Size-1;
                    newNode="NodeA";
                    System.out.println("switch "+newNode);
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
                    System.out.println("switch "+newNode);
                }
                else if(newCoord.y<0)
                {
                    newCoord.y=Size-1;
                    newNode="NodeB";
                    System.out.println("switch "+newNode);
                }
                break;
        }
        if(newNode.compareTo("")!=0) {
            //alert the other node that a player want to join his vicinity
            channelNodeComS.basicPublish(Nodename+"NS", newNode, null, (PlayerId + " STILL " + Integer.toString(newCoord.x) + " " + Integer.toString(newCoord.y)+" _ "+Nodename+"L").getBytes());
            return false;
        }
        return true;
    }

}
