package arbitre;

import java.util.List;
import java.util.Random;

import ultime.jeux.alg.NegaScout;
import ultime.moteur.Plateau;

/**
 * Vérifie la recherche de l'algo ultime : quand un coup gagnant existe, elle en joue un,
 * et elle rend la main dans le temps imparti.
 */
public class CheckUltimeRecherche {
    public static void main(String[] a) {
        Random r = new Random(5);
        int positionsGagnantes = 0, rates = 0, horsDelai = 0;
        NegaScout recherche = new NegaScout();
        for (int partie = 0; partie < 400 && positionsGagnantes < 40; partie++) {
            int[][] t = Arbitre.plateauInitial();
            Plateau p = new Plateau();
            int couleur = Arbitre.NOIR;
            for (int coup = 0; coup < 120; coup++) {
                List<String> coups = Arbitre.coupsLegaux(t, couleur);
                if (coups.isEmpty()) break;
                boolean gagnable = false;
                for (String c : coups) if (gagne(t, c, couleur)) gagnable = true;
                String joueur = couleur == Arbitre.NOIR ? "noir" : "blanc";
                if (gagnable) {
                    positionsGagnantes++;
                    long debut = System.currentTimeMillis();
                    String choisi = recherche.meilleurCoup(p, joueur, 200);
                    if (System.currentTimeMillis() - debut > 600) horsDelai++;
                    if (choisi == null || !gagne(t, choisi, couleur)) rates++;
                    break;
                }
                String c = coups.get(r.nextInt(coups.size()));
                jouer(t, c);
                p.play(c, joueur);
                if (Arbitre.connecte(t, 1) || Arbitre.connecte(t, -1)) break;
                couleur = -couleur;
            }
        }
        System.out.println("positions gagnantes : " + positionsGagnantes + ", coups gagnants ratés : " + rates
                + ", recherches hors délai : " + horsDelai);
        System.exit(positionsGagnantes > 0 && rates == 0 && horsDelai == 0 ? 0 : 1);
    }

    static void jouer(int[][] t, String c) {
        int[] m = Arbitre.lireCoup(c);
        t[m[2]][m[3]] = t[m[0]][m[1]];
        t[m[0]][m[1]] = 0;
    }

    /** Vrai si le coup relie tous les pions du joueur (règle de l'arbitre : le joueur qui joue gagne). */
    static boolean gagne(int[][] t, String c, int couleur) {
        int[][] u = new int[8][];
        for (int i = 0; i < 8; i++) u[i] = t[i].clone();
        jouer(u, c);
        return Arbitre.connecte(u, couleur);
    }
}
