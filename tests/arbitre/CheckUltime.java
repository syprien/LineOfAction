package arbitre;

import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import ultime.moteur.Plateau;

/**
 * Vérifie le plateau de l'algo ultime sur des parties aléatoires :
 * ses coups possibles sont exactement ceux de l'arbitre, et l'empreinte de Zobrist mise à jour
 * coup par coup est toujours égale à celle recalculée depuis zéro.
 */
public class CheckUltime {
    public static void main(String[] a) {
        Random r = new Random(3);
        int positions = 0, erreursCoups = 0, erreursHash = 0;
        for (int partie = 0; partie < 300; partie++) {
            int[][] t = Arbitre.plateauInitial();
            Plateau p = new Plateau();
            int couleur = Arbitre.NOIR;
            for (int coup = 0; coup < 80; coup++) {
                for (int camp : new int[]{Arbitre.NOIR, Arbitre.BLANC}) {
                    TreeSet<String> attendu = new TreeSet<>(Arbitre.coupsLegaux(t, camp));
                    TreeSet<String> obtenu = new TreeSet<>(p.coupsPossibles(camp == Arbitre.NOIR ? "noir" : "blanc"));
                    positions++;
                    if (!attendu.equals(obtenu) && erreursCoups++ < 3)
                        System.out.println("coups différents\n" + Arbitre.afficher(t) + "\narbitre=" + attendu + "\nultime=" + obtenu);
                }
                if (p.getHash() != Plateau.calculerHash(p)) erreursHash++;
                List<String> coups = Arbitre.coupsLegaux(t, couleur);
                if (coups.isEmpty() || Arbitre.connecte(t, 1) || Arbitre.connecte(t, -1)) break;
                String c = coups.get(r.nextInt(coups.size()));
                int[] m = Arbitre.lireCoup(c);
                t[m[2]][m[3]] = t[m[0]][m[1]];
                t[m[0]][m[1]] = 0;
                p.play(c, couleur == Arbitre.NOIR ? "noir" : "blanc");
                if (p.copy().getHash() != p.getHash()) erreursHash++;
                couleur = -couleur;
            }
        }
        System.out.println("positions : " + positions + ", erreurs de coups : " + erreursCoups + ", erreurs d'empreinte : " + erreursHash);
        System.exit(erreursCoups + erreursHash == 0 ? 0 : 1);
    }
}
