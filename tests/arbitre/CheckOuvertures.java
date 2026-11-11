package arbitre;

import ultime.devoirLOA.Ouvertures;
import ultime.moteur.Plateau;

/**
 * Vérifie la bibliothèque d'ouvertures de l'algo ultime : elle connaît la position de départ
 * (Noir) et toutes les positions après le premier coup de Noir (Blanc), et ses coups sont légaux.
 */
public class CheckOuvertures {
    public static void main(String[] a) {
        int connues = 0, manquantes = 0, illegales = 0;
        Plateau depart = new Plateau();
        String c0 = Ouvertures.coup(depart, "noir");
        if (c0 == null) manquantes++; else if (!depart.coupsPossibles("noir").contains(c0)) illegales++; else connues++;
        for (String premier : depart.coupsPossibles("noir")) {
            Plateau p = depart.copy();
            p.play(premier, "noir");
            String c = Ouvertures.coup(p, "blanc");
            if (c == null) manquantes++;
            else if (!p.coupsPossibles("blanc").contains(c)) illegales++;
            else connues++;
        }
        System.out.println("positions connues : " + connues + ", manquantes : " + manquantes + ", coups illégaux : " + illegales);
        System.exit(manquantes == 0 && illegales == 0 ? 0 : 1);
    }
}
