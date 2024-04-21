import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class PlateauGraphique extends JComponent implements KeyListener {

    Image img, empoi;

    Plateau plateau;

    int tailleCases;

    FenetreJeu fenetreJeu;

    public PlateauGraphique(FenetreJeu fenetreJeu, Plateau plateau) {
        this.fenetreJeu = fenetreJeu;

        this.plateau = plateau;
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D drawable = (Graphics2D) g;
        drawable.setStroke(new BasicStroke(2));
        drawable.setFont(new Font(TOOL_TIP_TEXT_KEY, 1, 15));

        int taille = Math.min(getSize().width, getSize().height);

        tailleCases = taille / plateau.lignes();

        int[][] cases = plateau.grille();
        for (int i = 0; i < plateau.colonnes(); i++) {
            for (int j = 0; j < plateau.lignes(); j++) {
                drawable.drawRect(i * tailleCases,j * tailleCases,tailleCases,tailleCases);
                if(cases[i][j] != 0){
                    drawable.setStroke(new BasicStroke(4));
                    drawable.drawRoundRect(i * tailleCases + tailleCases/13,j * tailleCases + tailleCases/13,tailleCases*9/11,tailleCases*9/11, tailleCases, tailleCases);
                    if(cases[i][j] == plateau.joueur){
                        drawable.setColor(Color.RED);
                    }else{
                        drawable.setColor(Color.WHITE);
                    }
                    drawable.fillRoundRect(i * tailleCases + tailleCases/13,j * tailleCases + tailleCases/13,tailleCases*9/11,tailleCases*9/11, tailleCases, tailleCases);
                    drawable.setColor(Color.BLACK);
                    drawable.drawString(""+cases[i][j],  i * tailleCases + tailleCases*3/10,  j * tailleCases + tailleCases*6/10);
                    drawable.setStroke(new BasicStroke(2));
                }
            }
        }
    }


    @Override
    public void keyTyped(KeyEvent keyListener) {
    }


    @Override
    public void keyPressed(KeyEvent keyListener) {
    }
    
    @Override
    public void keyReleased(KeyEvent keyListener) {
        switch (keyListener.getKeyCode()) {
            case KeyEvent.VK_UP:
                System.out.println("up");
            break;
    
            case KeyEvent.VK_DOWN:
                System.out.println("down");
            break;
    
            case KeyEvent.VK_LEFT:
                System.out.println("left");
            break;
    
            case KeyEvent.VK_RIGHT:
                System.out.println("right");
            break;
        
            default:
                System.out.println("Use arrows keys");
                break;
        }
        // int ligne = mouseEvent.getY() / tailleCases;
        // int colonne = mouseEvent.getX() / tailleCases;
    
        repaint();
    }

}


