package arbitre;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

/** Compare le générateur de coups de Rival à celui de l'arbitre sur des positions aléatoires. */
public class CheckRival {
    public static void main(String[] a) throws Exception {
        Class<?> rc = Class.forName("rival.Rival");
        Object rival = rc.getDeclaredConstructor().newInstance();
        Field fb = rc.getDeclaredField("b"); fb.setAccessible(true);
        Method gen = rc.getDeclaredMethod("gen", int.class, int[].class); gen.setAccessible(true);
        Method comps = rc.getDeclaredMethod("components", int.class); comps.setAccessible(true);
        Random r = new Random(1);
        int positions = 0, diffs = 0, connDiffs = 0;
        for (int game = 0; game < 300; game++) {
            int[][] t = Arbitre.plateauInitial();
            int couleur = Arbitre.NOIR;
            for (int ply = 0; ply < 80; ply++) {
                int[] b = (int[]) fb.get(rival);
                for (int q = 0; q < 64; q++) { int v = t[q & 7][q >> 3]; b[q] = v == Arbitre.NOIR ? 1 : v == Arbitre.BLANC ? 2 : 0; }
                for (int side : new int[]{Arbitre.NOIR, Arbitre.BLANC}) {
                    int rs = side == Arbitre.NOIR ? 1 : 2;
                    TreeSet<String> ref = new TreeSet<>(Arbitre.coupsLegaux(t, side));
                    int[] mv = new int[128];
                    int n = (int) gen.invoke(rival, rs, mv);
                    TreeSet<String> riv = new TreeSet<>();
                    for (int i = 0; i < n; i++) riv.add(Arbitre.nomCase(mv[i] & 7, (mv[i] & 63) >> 3) + "-" + Arbitre.nomCase((mv[i] >> 6) & 7, ((mv[i] >> 6) & 63) >> 3));
                    positions++;
                    if (!ref.equals(riv) || riv.size() != n) {
                        if (diffs++ < 3) System.out.println("DIFF side " + side + "\n" + Arbitre.afficher(t) + "\nref=" + ref + "\nriv=" + riv);
                    }
                    boolean rc1 = ((int) comps.invoke(rival, rs)) == 1;
                    if (rc1 != Arbitre.connecte(t, side)) connDiffs++;
                }
                List<String> moves = Arbitre.coupsLegaux(t, couleur);
                if (moves.isEmpty() || Arbitre.connecte(t, 1) || Arbitre.connecte(t, -1)) break;
                int[] m = Arbitre.lireCoup(moves.get(r.nextInt(moves.size())));
                t[m[2]][m[3]] = t[m[0]][m[1]]; t[m[0]][m[1]] = 0;
                couleur = -couleur;
            }
        }
        System.out.println("positions checked: " + positions + ", move-list differences: " + diffs + ", connection differences: " + connDiffs);
    }
}
