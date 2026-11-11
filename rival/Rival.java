package rival;

import net.ClientJeu;
import net.JoueurReseau;

import java.util.Random;

/**
 * Joueur de Lines of Action : recherche alpha-beta itérative avec table de transposition
 * (Zobrist), limitée en temps. Évaluation : différence de pièces, centralisation et
 * nombre de groupes connexes (8-connexité) de chaque camp.
 * Coordonnées : case = colonne A-H (0-7) et ligne 1-8 (0-7), index = col + 8*ligne.
 * Valeurs de case : 0 vide, 1 noir, 2 blanc.
 */
public class Rival implements JoueurReseau {

    /** Exception interne levée quand le temps de réflexion est écoulé. */
    static final class Abort extends RuntimeException {
        Abort() { super(null, null, false, false); }
    }

    private static final Abort ABORT = new Abort();
    private static final int WIN = 1000000, INF = 2000000;
    private static final int EXACT = 0, LOWER = 1, UPPER = 2;
    private static final int TT_BITS = 18, TT_MASK = (1 << TT_BITS) - 1;
    private static final int[] DX = {1, 0, 1, 1}, DY = {0, 1, 1, -1};
    private static final int[] CEN = new int[64];

    static {
        for (int q = 0; q < 64; q++) {
            int c = q & 7, r = q >> 3;
            CEN[q] = 14 - Math.abs(2 * c - 7) - Math.abs(2 * r - 7);
        }
    }

    private final int[] b = new int[64];
    private final long[][] z = new long[3][64];
    private final long zTurn;
    private long hash;
    private final long[] tKey = new long[1 << TT_BITS];
    private final int[] tDepth = new int[1 << TT_BITS], tScore = new int[1 << TT_BITS],
            tMove = new int[1 << TT_BITS], tFlag = new int[1 << TT_BITS];
    private final int[][] pm = new int[64][128];
    private final int[][] psc = new int[64][128];
    private final int[] mark = new int[64], stack = new int[64];
    private int markId;
    private final Random rnd = new Random(42);

    private int me = 1, turn = 1, tempsRestant = 300000, rootVal;
    private boolean over;
    private long deadline, nodes;

    public Rival() {
        for (int s = 0; s < 3; s++)
            for (int q = 0; q < 64; q++) z[s][q] = rnd.nextLong();
        zTurn = rnd.nextLong();
        for (int c = 1; c <= 6; c++) { b[c] = 1; b[c + 56] = 1; }      // noirs B1-G1, B8-G8
        for (int r = 1; r <= 6; r++) { b[8 * r] = 2; b[7 + 8 * r] = 2; } // blancs A2-A7, H2-H7
        for (int q = 0; q < 64; q++) hash ^= z[b[q]][q];
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage : java -jar Rival.jar <machine> <port> <id>");
            return;
        }
        new ClientJeu(args[0], Integer.parseInt(args[1]), new Rival(), args[2]);
    }

    // ---------------- Protocole ----------------

    @Override public void setJoueurIds(String[] lesJoueurIds) { }

    @Override public void setId(String joueurId) { me = colour(joueurId); }

    @Override public void premierJoueur(String joueurId) { turn = colour(joueurId); }

    @Override public void setTemp(int tps) { tempsRestant = tps; }

    @Override public boolean partieTerminee() { return over; }

    @Override public void disqualifier(String joueurId, String motif) { over = true; }

    @Override
    public void jouer(String joueurId, String coup) {
        String c = coup.trim();
        int m = sq(c, 0) | (sq(c, 3) << 6);
        int mover = b[m & 63];
        apply(m);
        turn = 3 - mover;
        over = components(1) == 1 || components(2) == 1;
    }

    @Override
    public String choisirCoup() {
        turn = me;
        long budget = Math.max(20, Math.min(tempsRestant / 25, tempsRestant - 300));
        deadline = System.nanoTime() + budget * 1000000L;
        int[] mv = new int[128];
        int n = gen(me, mv);
        if (n == 0) return "A1-A1";
        int[] snap = b.clone();
        long snapHash = hash;
        int best = mv[0];
        for (int d = 1; d <= 40; d++) {
            int bm;
            try {
                bm = searchRoot(d, mv, n);
            } catch (Abort e) {
                System.arraycopy(snap, 0, b, 0, 64);
                hash = snapHash;
                break;
            }
            best = bm;
            for (int i = 0; i < n; i++) if (mv[i] == bm) { mv[i] = mv[0]; mv[0] = bm; break; }
            if (rootVal > WIN / 2) break;
        }
        turn = me;
        return str(best);
    }

    // ---------------- Recherche ----------------

    private int searchRoot(int depth, int[] mv, int n) {
        int alpha = -INF, bestM = mv[0];
        for (int i = 0; i < n; i++) {
            int m = mv[i];
            int cap = apply(m);
            int v;
            if (components(me) == 1) v = WIN;
            else if (components(3 - me) == 1) v = -WIN;
            else {
                turn = 3 - me;
                v = -search(depth - 1, -INF, -alpha, 1);
                turn = me;
            }
            undo(m, cap);
            if (i == 0 || v > alpha) { alpha = v; bestM = m; }
        }
        rootVal = alpha;
        return bestM;
    }

    /** Valeur du point de vue du joueur dont c'est le trait (turn). */
    private int search(int depth, int alpha, int beta, int ply) {
        if ((++nodes & 1023) == 0 && System.nanoTime() > deadline) throw ABORT;
        int side = turn;
        if (depth <= 0) return eval(side);
        long key = hash ^ (side == 2 ? zTurn : 0);
        int idx = (int) (key & TT_MASK), ttMove = -1, a0 = alpha;
        if (tKey[idx] == key) {
            ttMove = tMove[idx];
            if (tDepth[idx] >= depth) {
                int sc = tScore[idx], f = tFlag[idx];
                if (f == EXACT || (f == LOWER && sc >= beta) || (f == UPPER && sc <= alpha)) return sc;
            }
        }
        int[] mv = pm[ply], sc = psc[ply];
        int n = gen(side, mv);
        if (n == 0) return -WIN + ply;
        for (int i = 0; i < n; i++) sc[i] = score(mv[i], ttMove);
        int best = -INF, bestM = -1;
        for (int i = 0; i < n; i++) {
            int j = i;
            for (int k = i + 1; k < n; k++) if (sc[k] > sc[j]) j = k;
            int tmp = mv[i]; mv[i] = mv[j]; mv[j] = tmp;
            tmp = sc[i]; sc[i] = sc[j]; sc[j] = tmp;
            int m = mv[i];
            int cap = apply(m);
            int v;
            if (components(side) == 1) v = WIN - ply;
            else if (components(3 - side) == 1) v = -(WIN - ply);
            else {
                turn = 3 - side;
                v = -search(depth - 1, -beta, -alpha, ply + 1);
                turn = side;
            }
            undo(m, cap);
            if (v > best) {
                best = v;
                bestM = m;
                if (v > alpha) {
                    alpha = v;
                    if (alpha >= beta) break;
                }
            }
        }
        int flag = best <= a0 ? UPPER : best >= beta ? LOWER : EXACT;
        tKey[idx] = key;
        tDepth[idx] = depth;
        tScore[idx] = best;
        tFlag[idx] = flag;
        tMove[idx] = bestM;
        return best;
    }

    private int score(int m, int ttMove) {
        if (m == ttMove) return 1 << 20;
        int t = (m >> 6) & 63;
        return CEN[t] + (b[t] != 0 ? 100 : 0);
    }

    /** Évaluation statique du point de vue de side. */
    private int eval(int side) {
        int opp = 3 - side, ps = 0, po = 0, cs = 0, co = 0;
        for (int q = 0; q < 64; q++) {
            if (b[q] == side) { ps++; cs += CEN[q]; }
            else if (b[q] == opp) { po++; co += CEN[q]; }
        }
        return 100 * (ps - po) + 6 * (cs - co) + 60 * (components(opp) - components(side));
    }

    // ---------------- Règles ----------------

    /** Joue m (sans contrôle de légalité) et renvoie la pièce capturée éventuelle. */
    private int apply(int m) {
        int f = m & 63, t = (m >> 6) & 63;
        int cap = b[t];
        put(t, b[f]);
        put(f, 0);
        return cap;
    }

    private void undo(int m, int cap) {
        int f = m & 63, t = (m >> 6) & 63;
        put(f, b[t]);
        put(t, cap);
    }

    private void put(int q, int v) {
        hash ^= z[b[q]][q] ^ z[v][q];
        b[q] = v;
    }

    /** Nombre de pièces (des deux couleurs) sur la ligne passant par s dans la direction (dx,dy). */
    private int line(int s, int dx, int dy) {
        int c = (s & 7) + dx, r = (s >> 3) + dy, k = 0;
        while (c >= 0 && c < 8 && r >= 0 && r < 8) {
            if (b[c + 8 * r] != 0) k++;
            c += dx;
            r += dy;
        }
        return k;
    }

    /** Génère les coups légaux de side dans mv, renvoie leur nombre. */
    private int gen(int side, int[] mv) {
        int n = 0, opp = 3 - side;
        for (int s = 0; s < 64; s++) {
            if (b[s] != side) continue;
            int c = s & 7, r = s >> 3;
            for (int d = 0; d < 4; d++) {
                int dx = DX[d], dy = DY[d];
                int cnt = 1 + line(s, dx, dy) + line(s, -dx, -dy);
                for (int sg = -1; sg <= 1; sg += 2) {
                    int tc = c + sg * dx * cnt, tr = r + sg * dy * cnt;
                    if (tc < 0 || tc > 7 || tr < 0 || tr > 7) continue;
                    boolean ok = true;
                    for (int k = 1; k < cnt && ok; k++) {
                        if (b[(c + sg * dx * k) + 8 * (r + sg * dy * k)] == opp) ok = false;
                    }
                    int t = tc + 8 * tr;
                    if (ok && b[t] != side) mv[n++] = s | (t << 6);
                }
            }
        }
        return n;
    }

    /** Nombre de groupes 8-connexes de pièces de side (1 = tout relié). */
    private int components(int side) {
        int comps = 0;
        markId++;
        for (int q = 0; q < 64; q++) {
            if (b[q] != side || mark[q] == markId) continue;
            comps++;
            int sp = 0;
            stack[sp++] = q;
            mark[q] = markId;
            while (sp > 0) {
                int p = stack[--sp], pc = p & 7, pr = p >> 3;
                for (int dc = -1; dc <= 1; dc++)
                    for (int dr = -1; dr <= 1; dr++) {
                        int nc = pc + dc, nr = pr + dr;
                        if (nc < 0 || nc > 7 || nr < 0 || nr > 7) continue;
                        int nq = nc + 8 * nr;
                        if (b[nq] == side && mark[nq] != markId) {
                            mark[nq] = markId;
                            stack[sp++] = nq;
                        }
                    }
            }
        }
        return comps;
    }

    // ---------------- Utilitaires ----------------

    private static int colour(String id) {
        return id.trim().toLowerCase().startsWith("n") ? 1 : 2;
    }

    private static int sq(String s, int i) {
        return (Character.toUpperCase(s.charAt(i)) - 'A') + 8 * (s.charAt(i + 1) - '1');
    }

    private static String str(int m) {
        return cell(m & 63) + "-" + cell((m >> 6) & 63);
    }

    private static String cell(int q) {
        return "" + (char) ('A' + (q & 7)) + (char) ('1' + (q >> 3));
    }
}
