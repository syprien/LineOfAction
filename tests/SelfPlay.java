import java.util.ArrayList;
import java.util.Random;

import ia.HeuristiqueLoa;
import jeux.alg.NegaScout;
import moteur.Plateau;

/** IA (NegaScout) contre un joueur aléatoire, en alternant les couleurs. */
public class SelfPlay {
    static boolean connected(Plateau p, byte c) {
        byte[][] t = p.getTab();
        boolean[][] seen = new boolean[8][8];
        int total = 0, si = -1, sj = -1;
        for (int i = 0; i < 8; i++) for (int j = 0; j < 8; j++) if (t[i][j] == c) { total++; si = i; sj = j; }
        if (total == 0) return false;
        java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<int[]>();
        q.add(new int[]{si, sj}); seen[si][sj] = true; int n = 0;
        while (!q.isEmpty()) {
            int[] a = q.poll(); n++;
            for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) {
                int x = a[0] + di, y = a[1] + dj;
                if (x >= 0 && y >= 0 && x < 8 && y < 8 && !seen[x][y] && t[x][y] == c) { seen[x][y] = true; q.add(new int[]{x, y}); }
            }
        }
        return n == total;
    }

    public static void main(String[] args) {
        int games = Integer.parseInt(args[0]);
        int depth = Integer.parseInt(args[1]);
        Random r = new Random(42);
        int aiWins = 0, rndWins = 0, draws = 0;
        long t0 = System.currentTimeMillis();
        for (int g = 0; g < games; g++) {
            String ai = (g % 2 == 0) ? "noir" : "blanc";
            String rnd = ai.equals("noir") ? "blanc" : "noir";
            Plateau p = new Plateau();
            String turn = "noir";
            int ply = 0;
            String winner = null;
            while (ply < 200) {
                String move;
                if (turn.equals(ai)) {
                    move = new NegaScout(new HeuristiqueLoa(), ai, depth).meilleurCoup(p, ai);
                } else {
                    ArrayList<String> l = p.coupsPossibles(rnd);
                    move = l.isEmpty() ? null : l.get(r.nextInt(l.size()));
                }
                if (move == null) break;
                if (!p.estValide(move, turn)) throw new IllegalStateException("illegal " + move);
                p.play(move, turn);
                ply++;
                if (p.finDePartie()) {
                    boolean n = connected(p, (byte) 1), b = connected(p, (byte) -1);
                    winner = (n && b) ? turn : (n ? "noir" : "blanc");
                    break;
                }
                turn = turn.equals("noir") ? "blanc" : "noir";
            }
            if (winner == null) draws++; else if (winner.equals(ai)) aiWins++; else rndWins++;
            System.err.println("game " + g + " AI=" + ai + " winner=" + winner + " plies=" + ply);
        }
        System.err.println("AI " + aiWins + " / random " + rndWins + " / unfinished " + draws
                + "  (" + (System.currentTimeMillis() - t0) / 1000 + "s)");
    }
}
