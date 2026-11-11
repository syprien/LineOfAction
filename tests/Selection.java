import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Sélection naturelle des poids de l'évaluation d'Ultime, comme le proposait la conclusion du
 * rapport de 2008. À chaque génération, un mutant (un ou deux poids du champion multipliés par un
 * facteur entre 1/2 et 2) affronte le champion sur des ouvertures nouvelles, dans les deux couleurs.
 * S'il marque au moins 56 %, un match de confirmation est joué ; il devient champion si son score
 * cumulé dépasse 55 %.
 * Usage : java Selection <générations> <ms par coup> <ouvertures par match> w1,w2,w3,w4,w5
 *   poids dans l'ordre : chaîne la plus longue, concentration, centre, pions, voisins.
 */
public class Selection {
    public static void main(String[] args) throws Exception {
        int generations = Integer.parseInt(args[0]);
        long ms = Long.parseLong(args[1]);
        int ouvertures = Integer.parseInt(args[2]);
        int[] champion = Arrays.stream(args[3].split(",")).mapToInt(Integer::parseInt).toArray();
        Random r = new Random(2008);
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
        long graine = 1000;
        for (int g = 1; g <= generations; g++) {
            int[] mutant = champion.clone();
            int nbMutations = 1 + r.nextInt(2);
            for (int k = 0; k < nbMutations; k++) {
                int i = r.nextInt(mutant.length);
                double facteur = Math.pow(2, 2 * r.nextDouble() - 1);
                mutant[i] = Math.max(0, (int) Math.round(mutant[i] * facteur + (r.nextBoolean() ? 1 : -1) * r.nextInt(2)));
            }
            if (Arrays.equals(mutant, champion)) { g--; continue; }
            double s1 = match(pool, mutant, champion, graine, ouvertures, ms);
            graine += ouvertures;
            String verdict;
            if (s1 >= 0.56) {
                double s2 = match(pool, mutant, champion, graine, ouvertures, ms);
                graine += ouvertures;
                double s = (s1 + s2) / 2;
                verdict = String.format("confirmation %.0f %%, cumul %.0f %% -> %s", 100 * s2, 100 * s, s > 0.55 ? "NOUVEAU CHAMPION" : "rejeté");
                if (s > 0.55) champion = mutant;
            } else verdict = "rejeté";
            System.out.printf("génération %2d : mutant %-18s contre champion : %.0f %%  %s%n",
                    g, str(mutant), 100 * s1, verdict);
            System.out.flush();
        }
        System.out.println("champion final : " + str(champion));
        pool.shutdown();
    }

    /** Score de w contre adversaire (1 par victoire, 1/2 par nul) sur ouvertures × 2 parties. */
    static double match(ExecutorService pool, int[] w, int[] adversaire, long graine, int ouvertures, long ms) throws Exception {
        List<Future<Integer>> parties = new ArrayList<>();
        for (int o = 0; o < ouvertures; o++)
            for (int couleur = 0; couleur < 2; couleur++) {
                final long gr = graine + o;
                final boolean noir = couleur == 0;
                parties.add(pool.submit(() -> Fitness.partie(w, adversaire, gr, noir, ms)));
            }
        double score = 0;
        for (Future<Integer> f : parties) score += (f.get() + 1) / 2.0;
        return score / parties.size();
    }

    static String str(int[] w) {
        StringBuilder s = new StringBuilder();
        for (int x : w) s.append(s.length() == 0 ? "" : ",").append(x);
        return s.toString();
    }
}
