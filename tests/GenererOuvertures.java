import ultime.jeux.alg.NegaScout;
import ultime.moteur.Plateau;

/**
 * Calcule la bibliothèque d'ouvertures de l'algo ultime : une recherche longue sur la position
 * de départ (Noir) et sur chaque position après le premier coup de Noir (Blanc).
 * Affiche les lignes à coller dans ultime/devoirLOA/Ouvertures.java.
 * Usage : java GenererOuvertures [millisecondes par position, 10000 par défaut]
 */
public class GenererOuvertures {
    public static void main(String[] args) {
        long budget = args.length > 0 ? Long.parseLong(args[0]) : 10000;
        Plateau depart = new Plateau();
        sortir(depart, "noir", budget);
        for (String premier : depart.coupsPossibles("noir")) {
            Plateau p = depart.copy();
            p.play(premier, "noir");
            sortir(p, "blanc", budget);
        }
    }

    private static void sortir(Plateau p, String joueur, long budget) {
        String coup = new NegaScout().meilleurCoup(p, joueur, budget);
        System.out.println("        ajouter(0x" + Long.toHexString(p.getHash()) + "L, \"" + joueur + "\", \"" + coup + "\");");
    }
}
