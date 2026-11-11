package ultime.moteur;

import java.util.ArrayList;

/**
 * ULTIME (2026), étape « rapide » : le plateau réécrit pour la vitesse. Les règles sont celles
 * du plateau de 2008 (moteur/Plateau.java), c'est la mécanique qui change :
 *  - un seul plateau, sur lequel la recherche JOUE puis ANNULE chaque coup (jouer / annuler),
 *    au lieu de copier les 64 cases pour chaque coup essayé ;
 *  - un coup est un entier (case de départ * 64 + case d'arrivée) au lieu d'une chaîne "B1-D3" ;
 *  - les pions de chaque couleur sont aussi rangés dans un « bitboard » : un long de 64 bits,
 *    un bit par case. Compter les pions d'une ligne devient un seul Long.bitCount(), et
 *    chercher un groupe de pions voisins se fait par décalages de bits, sans ArrayList.
 * Une case est numérotée colonne * 8 + ligne : A1 = 0, A2 = 1, ..., B1 = 8, ..., H8 = 63.
 * Les chaînes ("noir", "B1-D3") ne servent plus qu'à parler au serveur et aux tests.
 *
 * @author Syprien - tazzzz
 */
public class Plateau {

    public static final byte NOIR = 1;
    public static final byte BLANC = -1;

    // Hachage de Zobrist : mêmes nombres qu'avant (même graine, même ordre de tirage), donc les
    // empreintes, et la bibliothèque d'ouvertures qui en dépend, ne changent pas.
    private static final long[][] ZOBRIST = new long[2][64];

    // Masques des lignes : LIGNE[direction][case] contient toutes les cases de la ligne qui passe
    // par la case, dans cette direction. Directions (dc, dl) : verticale, horizontale, diagonale « / »,
    // diagonale « \ ».
    private static final int[] DC = {0, 1, 1, 1}, DL = {1, 0, 1, -1};
    private static final long[][] LIGNE = new long[4][64];

    /** VOISINS[case] : les 8 cases autour de la case. */
    public static final long[] VOISINS = new long[64];

    static {
        java.util.Random r = new java.util.Random(2008);
        for (int c = 0; c < 2; c++)
            for (int s = 0; s < 64; s++)
                ZOBRIST[c][s] = r.nextLong();
        for (int s = 0; s < 64; s++) {
            int c = s >> 3, l = s & 7;
            for (int d = 0; d < 4; d++)
                for (int k = -7; k <= 7; k++)
                    if (dedans(c + k * DC[d], l + k * DL[d])) LIGNE[d][s] |= 1L << (s + k * (8 * DC[d] + DL[d]));
            for (int dc = -1; dc <= 1; dc++)
                for (int dl = -1; dl <= 1; dl++)
                    if ((dc != 0 || dl != 0) && dedans(c + dc, l + dl)) VOISINS[s] |= 1L << (s + 8 * dc + dl);
        }
    }

    private final byte[] cases = new byte[64];
    private long noirs, blancs, hash;

    // Pile d'annulation : pour chaque coup joué, le coup et la couleur du pion éventuellement pris.
    private final int[] pileCoups = new int[1024];
    private final byte[] pilePris = new byte[1024];
    private int hauteur;

    public Plateau() {
        for (int i = 1; i < 7; i++) {
            poser(i, BLANC);          // colonne A, lignes 2 à 7
            poser(56 + i, BLANC);     // colonne H
            poser(i * 8, NOIR);       // ligne 1, colonnes B à G
            poser(i * 8 + 7, NOIR);   // ligne 8
        }
        hash = calculerHash(this);
    }

    private void poser(int s, byte couleur) {
        cases[s] = couleur;
        if (couleur == NOIR) noirs |= 1L << s; else blancs |= 1L << s;
    }

    private static boolean dedans(int c, int l) {
        return c >= 0 && c < 8 && l >= 0 && l < 8;
    }

    private static int indice(byte couleur) {
        return couleur == NOIR ? 0 : 1;
    }

    /** Empreinte recalculée depuis zéro (sert au démarrage et aux tests). */
    public static long calculerHash(Plateau p) {
        long h = 0;
        for (int s = 0; s < 64; s++)
            if (p.cases[s] != 0) h ^= ZOBRIST[indice(p.cases[s])][s];
        return h;
    }

    /** Empreinte de Zobrist de la position, mise à jour à chaque coup. */
    public long getHash() {
        return hash;
    }

    /** Les pions de cette couleur, un bit par case. */
    public long pions(byte couleur) {
        return couleur == NOIR ? noirs : blancs;
    }

    /** Le contenu d'une case : NOIR, BLANC ou 0. */
    public byte contenu(int s) {
        return cases[s];
    }

    // ------------------------------------------------------------------ coups

    /**
     * Range dans coups[] les coups légaux de cette couleur et renvoie leur nombre.
     * Règle de Lines of Action : un pion avance d'autant de cases qu'il y a de pions (des deux
     * couleurs) sur la ligne de son déplacement ; il peut sauter ses propres pions, pas ceux de
     * l'adversaire ; il peut prendre un pion adverse sur la case d'arrivée.
     */
    public int coups(byte couleur, int[] coups) {
        long amis = pions(couleur), occupe = noirs | blancs;
        int n = 0;
        for (long reste = amis; reste != 0; reste &= reste - 1) {
            int s = Long.numberOfTrailingZeros(reste), c = s >> 3, l = s & 7;
            for (int d = 0; d < 4; d++) {
                int pas = Long.bitCount(occupe & LIGNE[d][s]);
                for (int sens = -1; sens <= 1; sens += 2) {
                    int dc = sens * DC[d], dl = sens * DL[d];
                    if (!dedans(c + pas * dc, l + pas * dl)) continue;
                    int delta = 8 * dc + dl, arrivee = s + pas * delta;
                    if ((amis >>> arrivee & 1) != 0) continue;
                    boolean libre = true;
                    for (int k = 1; k < pas && libre; k++)
                        libre = cases[s + k * delta] != -couleur;
                    if (libre) coups[n++] = s << 6 | arrivee;
                }
            }
        }
        return n;
    }

    /** Joue le coup (départ * 64 + arrivée) ; annuler() le défait. */
    public void jouer(int coup) {
        int de = coup >>> 6, vers = coup & 63;
        byte pion = cases[de], pris = cases[vers];
        long bitDe = 1L << de, bitVers = 1L << vers;
        if (pris != 0) {
            hash ^= ZOBRIST[indice(pris)][vers];
            if (pris == NOIR) noirs ^= bitVers; else blancs ^= bitVers;
        }
        hash ^= ZOBRIST[indice(pion)][de] ^ ZOBRIST[indice(pion)][vers];
        if (pion == NOIR) noirs ^= bitDe | bitVers; else blancs ^= bitDe | bitVers;
        cases[vers] = pion;
        cases[de] = 0;
        pileCoups[hauteur] = coup;
        pilePris[hauteur++] = pris;
    }

    /** Annule le dernier coup joué. */
    public void annuler() {
        int coup = pileCoups[--hauteur], de = coup >>> 6, vers = coup & 63;
        byte pion = cases[vers], pris = pilePris[hauteur];
        long bitDe = 1L << de, bitVers = 1L << vers;
        hash ^= ZOBRIST[indice(pion)][de] ^ ZOBRIST[indice(pion)][vers];
        if (pion == NOIR) noirs ^= bitDe | bitVers; else blancs ^= bitDe | bitVers;
        cases[de] = pion;
        cases[vers] = pris;
        if (pris != 0) {
            hash ^= ZOBRIST[indice(pris)][vers];
            if (pris == NOIR) noirs ^= bitVers; else blancs ^= bitVers;
        }
    }

    // ------------------------------------------------------------------ groupes de pions

    /** Les 8 voisins de chaque case de x, et x lui-même, par décalages de bits. */
    private static long etendre(long x) {
        final long PAS_LIGNE_1 = 0xFEFEFEFEFEFEFEFEL, PAS_LIGNE_8 = 0x7F7F7F7F7F7F7F7FL;
        x |= (x << 1 & PAS_LIGNE_1) | (x >>> 1 & PAS_LIGNE_8);
        return x | x << 8 | x >>> 8;
    }

    /** Le groupe de pions voisins (8-connexité) qui contient les pions de depart, parmi pions. */
    public static long groupe(long depart, long pions) {
        long g = depart, avant;
        do {
            avant = g;
            g = etendre(g) & pions;
        } while (g != avant);
        return g;
    }

    /** Vrai si tous les pions de cette couleur forment un seul groupe. */
    public boolean relie(byte couleur) {
        long p = pions(couleur);
        return p != 0 && groupe(p & -p, p) == p;
    }

    // ------------------------------------------------------------------ interface chaînes

    /** "B1-D3" -> entier. */
    public static int lireCoup(String coup) {
        int de = (coup.charAt(0) - 'A') * 8 + coup.charAt(1) - '1';
        int vers = (coup.charAt(3) - 'A') * 8 + coup.charAt(4) - '1';
        return de << 6 | vers;
    }

    /** entier -> "B1-D3". */
    public static String ecrireCoup(int coup) {
        return nomCase(coup >>> 6) + "-" + nomCase(coup & 63);
    }

    private static String nomCase(int s) {
        return "" + (char) ('A' + (s >> 3)) + (char) ('1' + (s & 7));
    }

    public static byte couleur(String joueur) {
        return joueur.equalsIgnoreCase("noir") ? NOIR : BLANC;
    }

    public void play(String move, String player) {
        jouer(lireCoup(move));
    }

    public ArrayList<String> coupsPossibles(String cJoueur) {
        int[] t = new int[128];
        int n = coups(couleur(cJoueur), t);
        ArrayList<String> resultat = new ArrayList<String>();
        for (int k = 0; k < n; k++) resultat.add(ecrireCoup(t[k]));
        return resultat;
    }

    public boolean estValide(String move, String player) {
        return coupsPossibles(player).contains(move);
    }

    /**
     * CORRECTIF (2026) : la version de 2008 s'arrêtait après avoir examiné les DEUX premiers pions
     * rencontrés (compteur « ok »), même s'ils étaient de la même couleur. Sur le plateau de départ, ce
     * sont A2 et A3, deux pions blancs : seule la victoire de Blanc était détectée, celle de Noir jamais
     * tant que la colonne A gardait au moins deux pions blancs. Les deux couleurs sont maintenant testées.
     */
    public boolean finDePartie() {
        return relie(NOIR) || relie(BLANC);
    }

    public Plateau copy() {
        Plateau p = new Plateau();
        System.arraycopy(cases, 0, p.cases, 0, 64);
        p.noirs = noirs;
        p.blancs = blancs;
        p.hash = hash;
        return p;
    }

    public String toString() {
        StringBuilder ligne = new StringBuilder();
        for (int l = 7; l >= 0; l--) {
            for (int c = 0; c < 8; c++)
                ligne.append(cases[c * 8 + l] == NOIR ? 'X' : cases[c * 8 + l] == BLANC ? 'O' : '.').append(' ');
            ligne.append('\n');
        }
        return ligne.toString();
    }
}
