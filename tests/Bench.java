import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;

import ia.HeuristiqueLoa;
import jeux.alg.NegaScout;
import moteur.Plateau;

/** Temps moyen d'un coup de NegaScout par profondeur, sur des positions atteintes après des coups d'ouverture aléatoires. */
public class Bench {
    public static void main(String[] args) {
        PrintStream out = System.out;
        System.setOut(new PrintStream(new OutputStream() { public void write(int b) {} })); // masque l'affichage du nombre de nœuds
        int maxDepth = Integer.parseInt(args[0]);
        Random r = new Random(7);
        Plateau[] positions = new Plateau[5];
        for (int k = 0; k < positions.length; k++) {
            Plateau p = new Plateau();
            String turn = "noir";
            for (int m = 0; m < 6 + 2 * k; m++) {
                ArrayList<String> l = p.coupsPossibles(turn);
                p.play(l.get(r.nextInt(l.size())), turn);
                turn = turn.equals("noir") ? "blanc" : "noir";
            }
            positions[k] = p;
        }
        for (int d = 1; d <= maxDepth; d++) {
            long t0 = System.nanoTime();
            for (Plateau p : positions) new NegaScout(new HeuristiqueLoa(), "noir", d).meilleurCoup(p.copy(), "noir");
            double ms = (System.nanoTime() - t0) / 1e6 / positions.length;
            out.printf("depth %d (%d plies): %8.0f ms per move%n", d, d + 1, ms);
        }
    }
}
