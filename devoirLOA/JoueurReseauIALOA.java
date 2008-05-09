/**
* @author tazzzz
* 
*/

package devoirLOA;

import java.util.ArrayList;

import ia.HeuristiqueLoa;
import jeux.alg.NegaAB;
import jeux.alg.NegaScout;
import moteur.Plateau;
import net.JoueurReseau;

public class JoueurReseauIALOA implements JoueurReseau {
	private String IDjoueur;
	private String [] IDsJoueurs;
	private Plateau p;
	private boolean partieTerminee;
	private int nbCoup = 0;
	private int temp;
	private int increment;
	private int prof;
	private boolean coupRandom;
	
	public JoueurReseauIALOA(Plateau p) {
		this.p = p;
		this.partieTerminee=false;
		this.nbCoup = 0;
		this.increment = 0;
		this.prof = 3;
		this.coupRandom = false;
	}
	@Override
	public String choisirCoup() {
		this.prof = this.prof + this.increment;
		NegaScout algoNegaScout;
		java.util.Random r=new java.util.Random( ) ;
		/* on passe en mode de jeu aléatoire si :
		 * - on est en début de partie (les 3 premiers coups)
		 * - le temps restant est trop faible pour développer l'agorithme classique
		 * - on souhaite introduire une part de hasard dans le jeu de notre ia donc 
		 *   nous avons inclu une chance pour elle de jouer au hasard
		 */
		if(this.nbCoup < 3 || this.temp < 1000 || Math.random() < 0.05) {
			ArrayList<String> lst = p.coupsPossibles(this.IDjoueur);
			this.nbCoup++;
			this.coupRandom = true;
			return lst.get(r.nextInt(lst.size()-1));
		}
		this.coupRandom = false;
		this.nbCoup++;
		if(this.temp < 11000) {
			algoNegaScout = new NegaScout(new HeuristiqueLoa(),this.IDjoueur,1);
		} else {
			algoNegaScout = new NegaScout(new HeuristiqueLoa(),this.IDjoueur,this.prof);
		}
		return algoNegaScout.meilleurCoup(p,this.IDjoueur);
		/*
		 * Différentes fonctions utilisées pour tester les algorithmes implémentés
		 */
		//Minimax algoMiniMaxnew = new Minimax(new HeuristiqueLoa(),"blanc","noir");
		//return algoMiniMaxnew.meilleurCoup(p,"blanc");
		//NegaAB algoNagaAB = new NegaAB(new HeuristiqueLoa(),this.IDjoueur,this.temp,3);
		//return algoNagaAB.meilleurCoup(p);
	}
	
	
	@Override
	public void disqualifier(String joueurId, String Motif) {
		// TODO Auto-generated method stub
		partieTerminee = true;
	}

	@Override
	public void jouer(String joueurId, String coup) {
		//System.out.println("Coup joué par : "+joueurId+"  |  "+coup);
		if(joueurId == "noir") {
			this.p.play(coup,"noir");
		}
		else 
			this.p.play(coup,joueurId.toLowerCase());
	}
	
	@Override
	public boolean partieTerminee() {
		return p.finDePartie();
	}

	@Override
	public void premierJoueur(String joueurId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setId(String joueurId) {
		// TODO Auto-generated method stub
		this.IDjoueur = joueurId;
		
	}

	@Override
	public void setJoueurIds(String[] lesJoueurIds) {
		// TODO Auto-generated method stub
		this.IDsJoueurs = lesJoueurIds;
		
	}
	/**
	 * Mise à jour du temps restant pour le joueur.
	 * De plus, en fonction du temp utilise pour calculer le dernier coup
	 * la fonction adapte la profondeur de recherche de l'ia
	 * @param tps temps restant pour le joueur
	 */
	@Override
	public void setTemp(int tps) {
		if(this.temp != 0 && this.nbCoup > 3 && !this.coupRandom)
			if((this.temp - tps) < 900) {
				this.increment = 1;
			} else if((this.temp - tps) > 15000 && this.prof > 1) {
				this.increment = -1;
			} else if ((this.temp - tps) < 200) 
				this.increment = 2;
			  else
				this.increment = 0;
		this.temp = tps;
	}
	
}
