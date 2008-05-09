package devoirLOA;

import moteur.Plateau;
import net.ClientJeu;

public class ClientJoueurReseauIALOA {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 3)
            System.out.println("Mode d'appel : java ClientJoueurAleatoireLOA <Machine> <Port> <IdentifiantJoueur>");
        else {
        	Plateau p = new Plateau();
            new ClientJeu(args[0], 
            		      Integer.parseInt(args[1]), 
            		      new JoueurReseauIALOA(p),
            		      args[2]);
        }

	}

}
