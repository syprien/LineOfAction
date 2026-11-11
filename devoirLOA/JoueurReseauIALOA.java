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
			// CORRECTIF (2026) : r.nextInt(n) renvoie un nombre entre 0 et n-1 (n est EXCLU).
			// nextInt(lst.size()-1) ne pouvait donc jamais choisir le DERNIER coup légal, et quand
			// un seul coup était légal, il appelait nextInt(0), qui lève IllegalArgumentException.
			return lst.get(r.nextInt(lst.size()));
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
		// CORRECTIF (2026) : en Java, == entre deux String compare les OBJETS (références mémoire),
		// pas leur texte. Une String lue sur le réseau est un nouvel objet, donc
		// joueurId == "noir" était toujours faux. Pour du texte : equals()/equalsIgnoreCase().
		// Ici les deux branches faisaient de toute façon la même chose : une ligne suffit.
		this.p.play(coup, joueurId.toLowerCase());
	}
	
	@Override
	public boolean partieTerminee() {
		// CORRECTIF (2026) : disqualifier() positionne l'indicateur partieTerminee, mais cette méthode
		// l'ignorait et ne regardait que le plateau. Après une disqualification, le
		// client attendait des coups jusqu'à ce que le serveur ferme la connexion.
		return partieTerminee || p.finDePartie();
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
			// CORRECTIF (2026) : le test "< 200" venait APRÈS "< 900". Tout ce qui est sous 200 est aussi
			// sous 900 : la première branche gagnait toujours et "+2" était inatteignable.
			// Dans une chaîne if / else if, mettre la condition la plus précise en premier.
			if((this.temp - tps) < 200) {
				this.increment = 2;
			} else if((this.temp - tps) < 900) {
				this.increment = 1;
			} else if((this.temp - tps) > 15000 && this.prof > 1) {
				this.increment = -1;
			} else
				this.increment = 0;
		this.temp = tps;
	}
	
}
