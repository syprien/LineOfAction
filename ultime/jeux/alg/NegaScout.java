package ultime.jeux.alg;

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
 * Étape « rapide » : la recherche travaille sur UN seul plateau (jouer, explorer, annuler) et
 * manipule des coups entiers, rangés dans des tableaux alloués une fois pour toutes.
 *
 * @author Syprien
 */
public class NegaScout {

    private static final int INFINI = 1000000;
    private static final int GAGNE = HeuristiqueLoa.VICTOIRE / 2;
    private static final int EXACT = 0, AU_MOINS = 1, AU_PLUS = 2;
    private static final int TAILLE = 1 << 18;
    private static final long TRAIT_BLANC = 0x5DEECE66DL * 0x9E3779B97F4A7C15L;
    private static final int PLIS = 64;

    // Table de transposition : tableaux parallèles (pas d'objet créé pendant la recherche).
    private final long[] cle = new long[TAILLE];
    private final int[] profondeur = new int[TAILLE], valeur = new int[TAILLE], type = new int[TAILLE];
    private final int[] meilleur = new int[TAILLE];

    // Coups et notes de tri de chaque niveau de la recherche.
    private final int[][] coups = new int[PLIS][128], notes = new int[PLIS][128];

    private final HeuristiqueLoa h = new HeuristiqueLoa();
    private long finNs, noeuds;
    private boolean arret;
    private int scoreRacine;

    /**
     * Cherche le meilleur coup pour joueur (« noir » ou « blanc ») en au plus budgetMs millisecondes.
     * Le plateau est rendu dans l'état où il a été reçu.
     * @return le coup, ou null si le joueur ne peut pas jouer
     */
    public String meilleurCoup(Plateau p, String joueur, long budgetMs) {
        finNs = System.nanoTime() + budgetMs * 1000000L;
        arret = false;
        byte moi = Plateau.couleur(joueur);
        int n = p.coups(moi, coups[0]);
        if (n == 0) return null;
        int choix = coups[0][0];
        for (int d = 1; d <= 40; d++) {
            int m = racine(p, moi, d, n);
            if (m == 0) break;                    // profondeur interrompue : on garde la précédente
            choix = m;
            int[] c = coups[0];                   // le meilleur coup est exploré en premier ensuite
            int k = 0;
            while (c[k] != m) k++;
            System.arraycopy(c, 0, c, 1, k);
            c[0] = m;
            if (scoreRacine >= GAGNE || scoreRacine <= -GAGNE) break;  // issue certaine
        }
        return Plateau.ecrireCoup(choix);
    }

    /** Nombre de positions explorées depuis la création (pour les mesures). */
    public long noeuds() {
        return noeuds;
    }

    private int racine(Plateau p, byte joueur, int d, int n) {
        int alpha = -INFINI, choix = 0;
        for (int k = 0; k < n; k++) {
            int c = coups[0][k], v;
            p.jouer(c);
            if (k == 0) {
                v = -recherche(p, d - 1, -INFINI, -alpha, (byte) -joueur, 1);
            } else {
                v = -recherche(p, d - 1, -alpha - 1, -alpha, (byte) -joueur, 1);
                if (v > alpha && !arret) v = -recherche(p, d - 1, -INFINI, -alpha, (byte) -joueur, 1);
            }
            p.annuler();
            if (arret) return 0;
            if (k == 0 || v > alpha) {
                alpha = v;
                choix = c;
            }
        }
        scoreRacine = alpha;
        return choix;
    }

    private int recherche(Plateau p, int d, int alpha, int beta, byte joueur, int pli) {
        if ((++noeuds & 127) == 0 && System.nanoTime() > finNs) arret = true;
        if (arret) return 0;
        // ULTIME (2026) : partie finie. L'adversaire vient de jouer ; s'il a relié ses pions, il a
        // gagné (même si son coup relie aussi les nôtres : règle de l'arbitre). Une victoire proche
        // vaut mieux qu'une victoire lointaine, d'où le « + d ».
        if (p.relie((byte) -joueur)) return -HeuristiqueLoa.VICTOIRE - d;
        if (p.relie(joueur)) return HeuristiqueLoa.VICTOIRE + d;
        if (d <= 0 || pli >= PLIS) return h.eval(p, joueur, 4);

        long k = p.getHash() ^ (joueur == Plateau.BLANC ? TRAIT_BLANC : 0);
        int i = (int) (k & (TAILLE - 1));
        int coupMemorise = 0;
        if (cle[i] == k) {
            coupMemorise = meilleur[i];
            if (profondeur[i] >= d) {
                int v = valeur[i];
                if (type[i] == EXACT || (type[i] == AU_MOINS && v >= beta) || (type[i] == AU_PLUS && v <= alpha))
                    return v;
            }
        }

        int[] c = coups[pli];
        int n = p.coups(joueur, c);
        if (n == 0) return h.eval(p, joueur, 4);
        ordonner(c, notes[pli], n, coupMemorise, p);

        int alphaInitial = alpha, b = beta, choix = 0;
        for (int m = 0; m < n; m++) {
            p.jouer(c[m]);
            int v = -recherche(p, d - 1, -b, -alpha, (byte) -joueur, pli + 1);
            if (m > 0 && v > alpha && v < beta && d > 1 && !arret)
                v = -recherche(p, d - 1, -beta, -v, (byte) -joueur, pli + 1);
            p.annuler();
            if (arret) return 0;
            if (v > alpha || choix == 0) {
                if (v > alpha) alpha = v;
                choix = c[m];
            }
            if (alpha >= beta) break;
            b = alpha + 1;
        }

        cle[i] = k;
        profondeur[i] = d;
        valeur[i] = alpha;
        type[i] = alpha <= alphaInitial ? AU_PLUS : alpha >= beta ? AU_MOINS : EXACT;
        meilleur[i] = choix;
        return alpha;
    }

    /** Trie les coups (tri par insertion) : coup mémorisé, puis captures, puis arrivée près du centre. */
    private static void ordonner(int[] c, int[] note, int n, int coupMemorise, Plateau p) {
        for (int k = 0; k < n; k++) {
            int coup = c[k], vers = coup & 63, col = vers >> 3, lig = vers & 7;
            int x = coup == coupMemorise ? 1000
                    : (p.contenu(vers) != 0 ? 100 : 0) + 14 - Math.abs(2 * col - 7) - Math.abs(2 * lig - 7);
            int j = k;
            for (; j > 0 && note[j - 1] < x; j--) {
                note[j] = note[j - 1];
                c[j] = c[j - 1];
            }
            note[j] = x;
            c[j] = coup;
        }
    }
}
