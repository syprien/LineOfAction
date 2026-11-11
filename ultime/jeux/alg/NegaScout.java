package ultime.jeux.alg;

import java.util.ArrayList;

import ultime.ia.HeuristiqueLoa;
import ultime.moteur.Plateau;

/**
 * ULTIME (2026) : le NegaScout de 2008 (corrigé), avec les trois techniques modernes qui lui
 * manquaient :
 *  - approfondissement itératif : profondeur 1, puis 2, puis 3... jusqu'à épuisement du temps
 *    accordé au coup ; on joue le meilleur coup de la dernière profondeur terminée ;
 *  - table de transposition indexée par l'empreinte de Zobrist du plateau : une position déjà
 *    évaluée n'est pas réexplorée, et on retient son meilleur coup ;
 *  - ordre des coups : le meilleur coup retenu d'abord, puis les captures, puis les coups vers le
 *    centre. NegaScout parie que le premier coup est le meilleur : bien ordonner les coups est ce
 *    qui le rend plus rapide qu'un alpha-bêta simple.
 *
 * @author Syprien
 */
public class NegaScout {

    private static final int INFINI = 1000000;
    private static final int GAGNE = HeuristiqueLoa.VICTOIRE / 2;
    private static final int EXACT = 0, AU_MOINS = 1, AU_PLUS = 2;
    private static final int TAILLE = 1 << 18;
    private static final long TRAIT_BLANC = 0x5DEECE66DL * 0x9E3779B97F4A7C15L;

    // Table de transposition : tableaux parallèles (pas d'objet créé pendant la recherche).
    private final long[] cle = new long[TAILLE];
    private final int[] profondeur = new int[TAILLE], valeur = new int[TAILLE], type = new int[TAILLE];
    private final String[] meilleur = new String[TAILLE];

    private final HeuristiqueLoa h = new HeuristiqueLoa();
    private long finNs, noeuds;
    private boolean arret;
    private int scoreRacine;

    /**
     * Cherche le meilleur coup pour joueur (« noir » ou « blanc ») en au plus budgetMs millisecondes.
     * @return le coup, ou null si le joueur ne peut pas jouer
     */
    public String meilleurCoup(Plateau p, String joueur, long budgetMs) {
        finNs = System.nanoTime() + budgetMs * 1000000L;
        arret = false;
        ArrayList<String> coups = p.coupsPossibles(joueur);
        if (coups.isEmpty()) return null;
        String choix = coups.get(0);
        for (int d = 1; d <= 40; d++) {
            String m = racine(p, joueur, d, coups);
            if (m == null) break;                 // profondeur interrompue : on garde la précédente
            choix = m;
            coups.remove(m);
            coups.add(0, m);                      // le meilleur coup est exploré en premier ensuite
            if (scoreRacine >= GAGNE || scoreRacine <= -GAGNE) break;  // issue certaine
        }
        return choix;
    }

    private String racine(Plateau p, String joueur, int d, ArrayList<String> coups) {
        int alpha = -INFINI;
        String choix = null;
        boolean premier = true;
        for (String c : coups) {
            Plateau fils = p.copy();
            fils.play(c, joueur);
            int v;
            if (premier) {
                v = -recherche(fils, d - 1, -INFINI, -alpha, adversaire(joueur));
            } else {
                v = -recherche(fils, d - 1, -alpha - 1, -alpha, adversaire(joueur));
                if (v > alpha && !arret) v = -recherche(fils, d - 1, -INFINI, -alpha, adversaire(joueur));
            }
            if (arret) return null;
            if (premier || v > alpha) {
                alpha = v;
                choix = c;
            }
            premier = false;
        }
        scoreRacine = alpha;
        return choix;
    }

    private int recherche(Plateau p, int d, int alpha, int beta, String joueur) {
        if ((++noeuds & 127) == 0 && System.nanoTime() > finNs) arret = true;
        if (arret) return 0;
        if (p.finDePartie()) {
            // Une victoire proche vaut mieux qu'une victoire lointaine (et l'inverse pour une défaite).
            int v = h.eval(p, joueur, 4);
            return v >= GAGNE ? v + d : v <= -GAGNE ? v - d : v;
        }
        if (d <= 0) return h.eval(p, joueur, 4);

        long k = p.getHash() ^ (joueur.equals("blanc") ? TRAIT_BLANC : 0);
        int i = (int) (k & (TAILLE - 1));
        String coupMemorise = null;
        if (cle[i] == k) {
            coupMemorise = meilleur[i];
            if (profondeur[i] >= d) {
                int v = valeur[i];
                if (type[i] == EXACT || (type[i] == AU_MOINS && v >= beta) || (type[i] == AU_PLUS && v <= alpha))
                    return v;
            }
        }

        ArrayList<String> coups = p.coupsPossibles(joueur);
        if (coups.isEmpty()) return h.eval(p, joueur, 4);
        ordonner(coups, coupMemorise, p);

        int alphaInitial = alpha, b = beta;
        String choix = null;
        boolean premier = true;
        for (String c : coups) {
            Plateau fils = p.copy();
            fils.play(c, joueur);
            int v = -recherche(fils, d - 1, -b, -alpha, adversaire(joueur));
            if (!premier && v > alpha && v < beta && d > 1 && !arret)
                v = -recherche(fils, d - 1, -beta, -v, adversaire(joueur));
            if (arret) return 0;
            if (v > alpha || choix == null) {
                if (v > alpha) alpha = v;
                choix = c;
            }
            if (alpha >= beta) break;
            b = alpha + 1;
            premier = false;
        }

        cle[i] = k;
        profondeur[i] = d;
        valeur[i] = alpha;
        type[i] = alpha <= alphaInitial ? AU_PLUS : alpha >= beta ? AU_MOINS : EXACT;
        meilleur[i] = choix;
        return alpha;
    }

    /** Trie les coups : coup mémorisé, puis captures, puis arrivée près du centre. */
    private static void ordonner(ArrayList<String> coups, String coupMemorise, Plateau p) {
        final byte[][] t = p.getTab();
        coups.sort((x, y) -> note(y, coupMemorise, t) - note(x, coupMemorise, t));
    }

    private static int note(String c, String coupMemorise, byte[][] t) {
        if (c.equals(coupMemorise)) return 1000;
        int col = c.charAt(3) - 'A', lig = c.charAt(4) - '1';
        int centre = 14 - Math.abs(2 * col - 7) - Math.abs(2 * lig - 7);
        return (t[col][lig] != 0 ? 100 : 0) + centre;
    }

    private static String adversaire(String joueur) {
        return joueur.equalsIgnoreCase("noir") ? "blanc" : "noir";
    }
}
