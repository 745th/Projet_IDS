import java.util.Arrays;

public class Plateau {

    //bool[][] plateau; // Bool pour presence de joueur ou non 
    int[][] plateau; // int pour id de joueur ou 0 
    int col;
    int row;
    int joueur;

    public Plateau(int c, int r) {
        col = c;
        row = r;
        int joueur;
        plateau = new int[row][col];
        for (int i = 0; i < row; i++) {
            Arrays.fill(plateau[i], 0);
        }
    }

    public int lignes() {
        return row;
    }

    public int colonnes() {
        return col;
    }

    public int[][] grille() {
        return plateau;
    }

    public void afficher() {
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                System.out.print(plateau[i][j] + " ");
            }
            System.out.println();
        }
    }

}
