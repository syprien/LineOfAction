package ultime.devoirLOA;

import net.ClientJeu;

/** ULTIME (2026) : lancement du joueur, avec les mêmes arguments qu'en 2008. */
public class ClientJoueurReseauIALOA {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Mode d'appel : java -jar Ultime.jar <Machine> <Port> <IdentifiantJoueur>");
            return;
        }
        new ClientJeu(args[0], Integer.parseInt(args[1]), new JoueurReseauIALOA(), args[2]);
    }
}
