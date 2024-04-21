import javax.swing.JFrame;

public class test extends JFrame{
    public static void main(String[] args) {
        Plateau p = new Plateau(8, 8);
        try {
            FenetreJeu jeu = new FenetreJeu(p);
            // FenetreGraphique mainFen = new FenetreGraphique(p);
            jeu.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}