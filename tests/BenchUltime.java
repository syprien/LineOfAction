import java.util.ArrayList;
import java.util.Random;

import ultime.jeux.alg.NegaScout;
import ultime.moteur.Plateau;

/**
 * Temps d'une recherche d'Ultime à profondeur fixe sur 8 positions atteintes par des coups au hasard.
 * Le portage Rust (ultime bench <profondeur>) utilise les mêmes positions, le même générateur et
 * explore exactement les mêmes nœuds : seul le temps diffère. Deux passes : la première inclut le
 * temps de chauffe du compilateur JIT de Java.
 * Usage : java BenchUltime <profondeur>
 */
public class BenchUltime {
    public static void main(String[] args) {
        int profondeur = Integer.parseInt(args[0]);
        Random r = new Random(7);
        Plateau[] positions = new Plateau[8];
        String[] joueurs = new String[8];
        for (int k = 0; k < 8; k++) {
            Plateau p = new Plateau();
            String trait = "noir";
            for (int m = 0; m < 4 + 2 * k; m++) {
                ArrayList<String> l = p.coupsPossibles(trait);
                p.play(l.get(r.nextInt(l.size())), trait);
                trait = trait.equals("noir") ? "blanc" : "noir";
            }
            positions[k] = p;
            joueurs[k] = trait;
        }
        for (int passe = 1; passe <= 2; passe++) {
            long debut = System.nanoTime(), noeuds = 0;
            StringBuilder choix = new StringBuilder();
            for (int k = 0; k < 8; k++) {
                NegaScout ns = new NegaScout();
                choix.append(ns.meilleurCoup(positions[k], joueurs[k], 1000000, profondeur)).append(' ');
                noeuds += ns.noeuds();
            }
            double ms = (System.nanoTime() - debut) / 1e6;
            System.out.printf("passe %d : %.0f ms, %d noeuds, %.0f noeuds/ms  coups : %s%n", passe, ms, noeuds, noeuds / ms, choix);
        }
    }
}
