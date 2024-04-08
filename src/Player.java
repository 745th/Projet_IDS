import com.rabbitmq.client.*;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.security.*;
import java.security.cert.CertificateException;
import java.sql.Time;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
public class Player {


    private ConnectionFactory factory;
    Channel channelPublish;
    Channel channelListen;
    private DefaultConsumer Handler;

    String ID;

    public static void main(String[] args) throws IOException,TimeoutException{
        Player player = new Player();

    }
    public Player() throws IOException,TimeoutException
    {
        Init_Conn conn = new Init_Conn();
        Connect(conn.getNodename(1));
    }

    public void Connect(String Nodename) throws IOException,TimeoutException
    {
        long tid = new Date().getTime();
        ID=Long.toString(tid);
        factory = new ConnectionFactory();
        System.out.println(Nodename);
        factory.setHost("LocalHost");

        System.out.println("Connecting ...");

        Connection connection = factory.newConnection();
        System.out.println("Handler");
        channelPublish = connection.createChannel();
        channelListen = connection.createChannel();
        channelPublish.queueDeclare(Nodename, false, false, false, null);
        channelListen.queueDeclare(ID, false, false, false, null);
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
                    Work(consumerTag, envelope.getDeliveryTag(), message2);
                } catch (TimeoutException e) {
                    System.out.println(e);
                }
            }
        };
        channelListen.basicConsume(ID, false, Handler);
        channelPublish.basicPublish("", Nodename, null, "Hola raph".getBytes());
    }

    public void Work(String consumer,long deliveryTag, String message) throws IOException, TimeoutException
    {
        System.out.println("work");
    }

    private SSLContext SSlconn() throws IOException
    {
        SSLContext c;
        try {
            char[] keyPassphrase = "testowaw".toCharArray();
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(new FileInputStream("/path/to/client_key.p12"), keyPassphrase);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, keyPassphrase);

            char[] trustPassphrase = "rabbitstore".toCharArray();
            KeyStore tks = KeyStore.getInstance("JKS");
            tks.load(new FileInputStream("/path/to/trustStore"), trustPassphrase);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(tks);

            c = SSLContext.getInstance("TLSv1.2");
            c.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        }catch(FileNotFoundException | NoSuchAlgorithmException |
               CertificateException | KeyStoreException |
               KeyManagementException | UnrecoverableKeyException e)
        {
            System.out.println(e);
        }
        return NU;
    }

}
