import javax.swing.*;
import java.awt.*;

public class FenetreJeu  extends JFrame{

    JFrame frame;

    PlateauGraphique plateauGraphique;

    Color cyan = new Color(0,200,255); 
    Color rose = new Color(200,0,255); 
    Color green = new Color(150,255,0); 
    Color yellow = new Color(255,200,0); 

    public FenetreJeu(Plateau plateau) {
        frame = new JFrame("Jeu en cours");
        frame.setSize(new Dimension(600, 600));

        JPanel panel = (JPanel) frame.getContentPane();
        panel.setLayout(new BorderLayout());
        panel.setBackground(new Color(200,0,255));

        plateauGraphique = new PlateauGraphique(this, plateau);
        frame.addKeyListener(plateauGraphique);
        panel.add(plateauGraphique);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

}
