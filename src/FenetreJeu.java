import javax.swing.*;
import java.awt.*;

public class FenetreJeu extends JFrame{

    JFrame frame;
    JPanel panel;
    PlateauGraphique plateauGraphique;
    Player play;

    Color cyan = new Color(0,200,255);
    Color green = new Color(150,255,0);
    Color pink = new Color(200,0,255);
    Color yellow = new Color(255,200,0);

    public FenetreJeu(Plateau plateau, Player play) {
        this.play = play;
        frame = new JFrame("Jeu en cours");
        frame.setSize(new Dimension(600, 600));

        panel = (JPanel) frame.getContentPane();
        panel.setLayout(new BorderLayout());
        panel.setBackground(Color.GRAY);

        plateauGraphique = new PlateauGraphique(this, plateau,play);
        frame.addKeyListener(plateauGraphique);
        panel.add(plateauGraphique);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                play.Disconnect();
                System.exit(0);
            }
        });
    }

    public void updateBackground() {
        Color b_color;
        switch (play.Nodename) {
            case "NodeA":
                b_color = cyan;
                break;
            case "NodeB":
                b_color = green;
                break;
            case "NodeC":
                b_color = pink;
                break;
            case "NodeD":
                b_color = yellow;
                break;
            default:
                b_color = Color.GRAY;
        }
        panel.setBackground(b_color);
    }
}
