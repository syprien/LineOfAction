package ultime.ia;

import ultime.moteur.Plateau;

/**
 * Cette classe regrouper tout les fonctions qui compose l'heuristique à la base de
 * l'ia de notre jeu de LOA
 *
 * ULTIME (2026), étape « rapide » : les MÊMES critères et les MÊMES valeurs qu'en 2008
 * (ia/HeuristiqueLoa.java, version corrigée), calculés sur les bitboards du nouveau plateau, sans
 * créer une seule ArrayList ni un seul Couple. tests/arbitre/CheckUltimeEval vérifie, sur des
 * milliers de positions, que eval() renvoie exactement le même nombre que la version de 2008.
 *
 * @author tazzzz-syprien
 */
public class HeuristiqueLoa {
    // CORRECTIF (2026) : score d'une position gagnée. C'était Integer.MAX_VALUE, voir ligne_max().
    static final public int VICTOIRE = 10000;

    // Table de centralisation de 2008, réindexée case = colonne * 8 + ligne.
    private static final int[] VALEUR_2008 = {
            -16, -8, -4, -4, -4, -4, -8, -16,
            -8, 1, 1, 1, 1, 1, 1, -8,
            -2, 1, 6, 6, 6, 6, 1, -4,
            -2, 1, 6, 12, 12, 6, 1, -4,
            -2, 1, 6, 12, 12, 6, 1, -4,
            -2, 1, 6, 6, 6, 6, 1, -4,
            -8, 1, 1, 1, 1, 1, 1, -8,
            -16, -8, -4, -4, -4, -4, -8, -16,
    };
    private static final int[] VALEUR = new int[64];

    static {
        for (int s = 0; s < 64; s++) VALEUR[s] = VALEUR_2008[(s & 7) * 8 + (s >> 3)];
    }

    /**
     * Fonction evaluant un plateau du jeu LOA, du point de vue de joueur (somme nulle :
     * eval(p, NOIR) == -eval(p, BLANC)).
     * @param niveau niveau d'heuristique a applique (4 en partie)
     */
    public int eval(Plateau p, byte ami, int niveau) {
        byte enemi = (byte) -ami;
        long a = p.pions(ami), e = p.pions(enemi);
        int chaine = ligne_max(a) - ligne_max(e);
        if (niveau == 1) return chaine;
        int concentration = -evaluation(a) + evaluation(e);
        if (niveau == 2) return chaine + concentration;
        int centre = centre(a) - centre(e);
        if (niveau != 4) return chaine + concentration + centre;
        // CORRECTIF (2026) : deux critères mineurs, comme le prévoit le rapport, au lieu de
        // (ennemis² - amis) / voisins (voir ia/HeuristiqueLoa.java).
        int pions = nbPion(a, e), voisins = nb_voisin(a) - nb_voisin(e);
        if (poids == null) return chaine + concentration + centre + pions + voisins;
        return poids[0] * chaine + poids[1] * concentration + poids[2] * centre + poids[3] * pions + poids[4] * voisins;
    }

    // ULTIME (2026), étape « poids » : un coefficient devant chaque critère du niveau 4 (chaîne,
    // concentration, centre, pions, voisins). Sans poids, c'est la somme de 2008.
    // POIDS : les poids choisis par sélection naturelle (tests/Selection.java), partis de
    // 10,10,10,1,1 : la chaîne la plus longue compte plus, la centralisation moins.
    public static final int[] POIDS = {13, 10, 6, 1, 1};
    private final int[] poids;

    public HeuristiqueLoa() {
        this.poids = null;
    }

    public HeuristiqueLoa(int... poids) {
        this.poids = poids.clone();
    }

    /** Version chaînes, pour les tests : joueur vaut "noir" ou "blanc". */
    public int eval(Plateau p, String joueur, int niveau) {
        return eval(p, Plateau.couleur(joueur), niveau);
    }

    /**
     * Taille du plus grand groupe de pions voisins, ou VICTOIRE si ce groupe contient tous les pions.
     * (La remarque de 2008 est conservée : avec 0 pion, 0 == 0 et la fonction renvoie aussi VICTOIRE.)
     */
    private static int ligne_max(long pions) {
        int max = 0;
        for (long reste = pions; reste != 0; ) {
            long g = Plateau.groupe(reste & -reste, pions);
            max = Math.max(max, Long.bitCount(g));
            reste &= ~g;
        }
        return max == Long.bitCount(pions) ? VICTOIRE : max;
    }

    /** Concentration : somme des distances au barycentre, moins le minimum incompressible (mouv_min). */
    private static int evaluation(long pions) {
        int n = Long.bitCount(pions), sc = 0, sl = 0;
        for (long r = pions; r != 0; r &= r - 1) {
            int s = Long.numberOfTrailingZeros(r);
            sc += s >> 3;
            sl += s & 7;
        }
        int bc = 0, bl = 0;
        if (n != 0) {
            // CORRECTIF (2026) : division en float avant Math.round (voir la version corrigée).
            bc = Math.round((float) sc / n);
            bl = Math.round((float) sl / n);
        }
        int res = 0;
        for (long r = pions; r != 0; r &= r - 1) {
            int s = Long.numberOfTrailingZeros(r);
            res += Math.max(Math.abs((s >> 3) - bc), Math.abs((s & 7) - bl));
        }
        return mouv_min(res, n);
    }

    /** retire les distances normales pour former un bloc (les pièces ne peuvent se chevaucher) */
    private static int mouv_min(int res, int pion) {
        if (pion == 1) return res;
        if (pion <= 9) return res - pion;
        return res - 8 - (2 * (pion - 8));
    }

    private static int centre(long pions) {
        int res = 0, n = Long.bitCount(pions);
        for (long r = pions; r != 0; r &= r - 1) res += VALEUR[Long.numberOfTrailingZeros(r)];
        return n == 0 ? 0 : res / n;
    }

    /** Pions ennemis - pions amis (« il serait mal avisé d'en capturer un trop grand nombre »). */
    private static int nbPion(long amis, long ennemis) {
        return Long.bitCount(ennemis) - Long.bitCount(amis);
    }

    /** Nombre moyen de voisins de la même couleur, par pion. */
    private static int nb_voisin(long pions) {
        int res = 0, n = Long.bitCount(pions);
        for (long r = pions; r != 0; r &= r - 1)
            res += Long.bitCount(Plateau.VOISINS[Long.numberOfTrailingZeros(r)] & pions);
        return n == 0 ? 0 : res / n;
    }
}
