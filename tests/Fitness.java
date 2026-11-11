import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import rival.Rival;
import ultime.ia.HeuristiqueLoa;
import ultime.jeux.alg.NegaScout;
import ultime.moteur.Plateau;

/**
 * Test de fitness rapide des poids de l'évaluation d'Ultime : chaque variante joue contre un
 * adversaire fixe, en mémoire, sans réseau. Chaque ouverture aléatoire est jouée deux fois, une fois
 * avec chaque couleur, pour que l'ouverture ne favorise personne.
 * Usage : java Fitness <base|rival> <ms par coup> <ouvertures> w1,w2,w3,w4,w5 ...
 *   base  : la version de 2008 (poids 1,1,1,1,1) ; rival : l'IA écrite par Claude Haiku.
 *   poids dans l'ordre : chaîne la plus longue, concentration, centre, pions, voisins.
 */
public class Fitness {
    static final int[] BASE = {1, 1, 1, 1, 1};

    public static void main(String[] args) throws Exception {
        boolean contreRival = args[0].equals("rival");
        long ms = Long.parseLong(args[1]);
        int ouvertures = Integer.parseInt(args[2]);
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 3));
        for (int k = 3; k < args.length; k++) {
            String[] s = args[k].split(",");
            int[] w = new int[5];
            for (int i = 0; i < 5; i++) w[i] = Integer.parseInt(s[i]);
            List<Future<Integer>> parties = new ArrayList<>();
            for (int o = 0; o < ouvertures; o++)
                for (int couleur = 0; couleur < 2; couleur++) {
                    final long graine = o;
                    final boolean varianteNoire = couleur == 0;
                    parties.add(pool.submit(() -> partie(w, contreRival ? null : BASE, graine, varianteNoire, ms)));
                }
            int v = 0, n = 0, d = 0;
            for (Future<Integer> f : parties) {
                int r = f.get();
                if (r > 0) v++; else if (r == 0) n++; else d++;
            }
            System.out.printf("poids %-14s contre %s : %2d victoires, %2d nuls, %2d défaites  -> %.0f %%%n",
                    args[k], args[0], v, n, d, 100 * (v + 0.5 * n) / parties.size());
        }
        pool.shutdown();
    }

    /** Joue une partie de la variante w contre les poids adversaire (null : contre Rival) ; renvoie 1 si la variante gagne, -1 si elle perd, 0 si nulle. */
    static int partie(int[] w, int[] adversaire, long graine, boolean varianteNoire, long ms) {
        boolean contreRival = adversaire == null;
        Plateau p = new Plateau();
        Rival rival = new Rival();
        Random r = new Random(graine);
        byte trait = Plateau.NOIR, couleurVariante = varianteNoire ? Plateau.NOIR : Plateau.BLANC;
        rival.setId(varianteNoire ? "blanc" : "noir");
        int[] t = new int[128];
        for (int k = 0; k < 4; k++) {                 // ouverture : 4 demi-coups au hasard
            String c = Plateau.ecrireCoup(t[r.nextInt(p.coups(trait, t))]);
            p.play(c, null);
            rival.jouer(nom(trait), c);
            trait = (byte) -trait;
        }
        NegaScout variante = new NegaScout(new HeuristiqueLoa(w)), base = new NegaScout(new HeuristiqueLoa(contreRival ? BASE : adversaire));
        for (int coup = 0; coup < 200; coup++) {
            String c;
            if (trait == couleurVariante) c = variante.meilleurCoup(p, nom(trait), ms);
            else if (!contreRival) c = base.meilleurCoup(p, nom(trait), ms);
            else {
                rival.setTemp((int) ms * 25);         // Rival accorde 1/25 de son temps restant à un coup
                c = rival.choisirCoup();
                if (c.equals("A1-A1")) c = null;      // Rival passe quand il n'a aucun coup
            }
            if (c != null) {
                p.play(c, null);
                rival.jouer(nom(trait), c);
            }
            // Règle de l'arbitre : si le coup relie les deux camps, celui qui vient de jouer gagne.
            byte gagnant = p.relie(trait) ? trait : p.relie((byte) -trait) ? (byte) -trait : 0;
            if (gagnant != 0) return gagnant == couleurVariante ? 1 : -1;
            trait = (byte) -trait;
        }
        return 0;
    }

    static String nom(byte couleur) {
        return couleur == Plateau.NOIR ? "noir" : "blanc";
    }
}
