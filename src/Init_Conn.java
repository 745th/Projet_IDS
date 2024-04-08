import java.util.Random;
public class Init_Conn {

    String[] UriList;

    public Init_Conn()
    {
        UriList = new String[4];
        UriList[0]="nodeA";//"amqps://jldrpdok:8C9KJ8G4v3BKy_fA1xs10zw22AsAl_rr@rattlesnake.rmq.cloudamqp.com/jldrpdok";
        UriList[1]="nodeB";//"amqps://yfnszspt:sn1e2lYmrjqekmkGYR2WHsySWULUPvnB@rattlesnake.rmq.cloudamqp.com/yfnszspt"; //Instance
        UriList[2]="nodeC";//"amqps://vpagfpmc:wcQLsc6CD9DDUsDlz0WV9zuPRX6w8B_p@rattlesnake.rmq.cloudamqp.com/vpagfpmc";
        UriList[3]="nodeD";//"amqps://sigxapzn:zfU4BEKDm_wcNFNjw3_BkX6R1j3WqxcX@rattlesnake.rmq.cloudamqp.com/sigxapzn";
    }

    public String getNodename(int id)
    {
        return UriList[id];
    }

    public String getRandomUri()
    {
        Random rand=new Random();
        int randomNum = rand.nextInt(0,3);
        System.out.println("ID Serveur: "+randomNum);
        return getNodename(randomNum);
    }
}
