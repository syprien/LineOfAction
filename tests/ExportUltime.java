import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import ultime.ia.HeuristiqueLoa;
import ultime.jeux.alg.NegaScout;
import ultime.moteur.Plateau;

/**
 * Exporte des positions de référence pour vérifier le portage Rust d'Ultime (ultime-rs/).
 * Une ligne par position, champs séparés par des espaces :
 *   cases (64 caractères X, O ou ., case = colonne * 8 + ligne), trait (noir ou blanc),
 *   empreinte de Zobrist (hexadécimal), évaluation avec les POIDS, évaluation de 2008 (somme),
 *   coups possibles triés (séparés par des virgules, - si aucun),
 *   coup choisi et nombre de positions explorées par une recherche à profondeur 4 (- - si non calculée).
 * Usage : java ExportUltime <fichier> [parties]
 */
public class ExportUltime {
    public static void main(String[] args) throws Exception {
        int parties = args.length > 1 ? Integer.parseInt(args[1]) : 200;
        Random r = new Random(42);
        HeuristiqueLoa poids = new HeuristiqueLoa(HeuristiqueLoa.POIDS), somme = new HeuristiqueLoa();
        int lignes = 0;
        try (PrintWriter out = new PrintWriter(args[0], "UTF-8")) {
            for (int partie = 0; partie < parties; partie++) {
                Plateau p = new Plateau();
                String joueur = "noir";
                for (int coup = 0; coup < 100 && !p.finDePartie(); coup++) {
                    StringBuilder cases = new StringBuilder();
                    for (int s = 0; s < 64; s++)
                        cases.append(p.contenu(s) == Plateau.NOIR ? 'X' : p.contenu(s) == Plateau.BLANC ? 'O' : '.');
                    ArrayList<String> coups = p.coupsPossibles(joueur);
                    Collections.sort(coups);
                    String recherche = "- -";
                    if (lignes % 25 == 0) {
                        NegaScout ns = new NegaScout();
                        String c = ns.meilleurCoup(p, joueur, 1000000, 4);
                        recherche = (c == null ? "-" : c) + " " + ns.noeuds();
                    }
                    out.println(cases + " " + joueur + " " + Long.toHexString(p.getHash()) + " "
                            + poids.eval(p, joueur, 4) + " " + somme.eval(p, joueur, 4) + " "
                            + (coups.isEmpty() ? "-" : String.join(",", coups)) + " " + recherche);
                    lignes++;
                    if (coups.isEmpty()) break;
                    p.play(coups.get(r.nextInt(coups.size())), joueur);
                    joueur = joueur.equals("noir") ? "blanc" : "noir";
                }
            }
        }
        System.out.println(lignes + " positions exportées");
    }
}
