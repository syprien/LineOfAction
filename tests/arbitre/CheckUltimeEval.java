package arbitre;

import java.util.ArrayList;
import java.util.Random;

import ultime.moteur.Plateau;

/**
 * Vérifie le plateau rapide de l'algo ultime sur des parties aléatoires :
 *  - son évaluation donne exactement le même nombre que celle de 2008 (ia.HeuristiqueLoa, version
 *    corrigée, sur le plateau de 2008), pour les deux couleurs et les quatre niveaux ;
 *  - jouer puis annuler chaque coup possible rend le plateau et l'empreinte de départ ;
 *  - finDePartie() de 2008 (corrigé) et celui du plateau rapide sont toujours d'accord.
 */
public class CheckUltimeEval {
    public static void main(String[] a) {
        Random r = new Random(11);
        ia.HeuristiqueLoa h2008 = new ia.HeuristiqueLoa();
        ultime.ia.HeuristiqueLoa h = new ultime.ia.HeuristiqueLoa();
        int positions = 0, erreursEval = 0, erreursAnnuler = 0, erreursFin = 0;
        int[] t = new int[128];
        for (int partie = 0; partie < 300; partie++) {
            moteur.Plateau p2008 = new moteur.Plateau();
            Plateau p = new Plateau();
            String joueur = "noir";
            for (int coup = 0; coup < 80; coup++) {
                if (p2008.finDePartie() != p.finDePartie()) erreursFin++;
                if (p.finDePartie()) break;
                positions++;
                for (String camp : new String[]{"noir", "blanc"})
                    for (int niveau = 1; niveau <= 4; niveau++) {
                        int attendu = h2008.eval(p2008, camp, niveau), obtenu = h.eval(p, camp, niveau);
                        if (attendu != obtenu && erreursEval++ < 3)
                            System.out.println(p + camp + " niveau " + niveau + " : 2008=" + attendu + " rapide=" + obtenu);
                    }
                String avant = p.toString();
                long hash = p.getHash();
                int n = p.coups(Plateau.couleur(joueur), t);
                for (int k = 0; k < n; k++) {
                    p.jouer(t[k]);
                    p.annuler();
                    if (!p.toString().equals(avant) || p.getHash() != hash) erreursAnnuler++;
                }
                ArrayList<String> coups = p.coupsPossibles(joueur);
                if (coups.isEmpty()) break;
                String c = coups.get(r.nextInt(coups.size()));
                p.play(c, joueur);
                p2008.play(c, joueur);
                joueur = joueur.equals("noir") ? "blanc" : "noir";
            }
        }
        System.out.println("positions : " + positions + ", évaluations différentes : " + erreursEval
                + ", annulations ratées : " + erreursAnnuler + ", fins de partie différentes : " + erreursFin);
        System.exit(erreursEval + erreursAnnuler + erreursFin == 0 ? 0 : 1);
    }
}
