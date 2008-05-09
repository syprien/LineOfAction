package jeux.alg;

import ia.HeuristiqueLoa;

import java.util.ArrayList;
import java.util.Iterator;

import moteur.Plateau;
/**
 * Fonction d'implementation de l'agorithme NegaScout
 * 
 * @author tazzzz-syprien
 * 
 *
 */
public class NegaScout {
	private final static int PROFMAXDEFAUT = 3;
	private int profMax = PROFMAXDEFAUT;
	
	private HeuristiqueLoa h;
	private String joueurMax;
	private int nb;

	/* constructeurs */
	/**
	 * constructeur de la classe NegaAB
	 * @param h heuristique utilisé
	 * @param joueurMax joueur dont on souhaite maximisé le score
	 */
	public NegaScout(HeuristiqueLoa h,String joueurMax){
		this.h = h;
		this.joueurMax = joueurMax.toLowerCase();
		this.nb = 0;
	}

	/**
	 * constructeur de la classe NegaAB
	 * @param h heuristique utilisé
	 * @param joueurMax joueur dont on souhaite maximisé le score
	 * @param profMaxi horizon limite de l'algorithme 
	 */
	public NegaScout(HeuristiqueLoa h, String joueurMax, int profMaxi){
		this.h = h;
		this.joueurMax = joueurMax;
		this.profMax = profMaxi;
		this.nb = 0;
	}

	public String toString() {
		return "NegaScout";
	}
	
	/**
	 * Cette fonction retourne le meilleur coup possible estimé pour le joueur j
	 * @param p plateau de jeu
	 * @return meilleur coup possible estimé pour le joueur j
	 */
	public String meilleurCoup(Plateau p, String j) {
		int nbNoeud = 0;
		ArrayList<String> lst = p.coupsPossibles(this.joueurMax);
		
		if(lst.size() == 0) {
			return null;
		}
		int meilleur = Integer.MIN_VALUE;
		
		int aux;
		String coup = null,meilleurCoup = null;
		Iterator<String> itr = lst.iterator();
		while (itr.hasNext()) {
		      coup = itr.next();
		      Plateau nouveauPlateau = p.copy();
		      nouveauPlateau.play(coup,this.joueurMax);
		      aux = negasct(nouveauPlateau,Integer.MIN_VALUE,Integer.MAX_VALUE,0);
		      if(meilleur < aux) {
		    	  meilleur = aux;
		    	  meilleurCoup = coup; 
		      }
		}
		System.out.println("nombre de noeud exploré : " + this.nb);
		return meilleurCoup;
	}
	
	/**
	 * maximisation du score du joueur
	 * @param p plateau de jeu
	 * @param alpha borne de la fenetre
	 * @param beta borne de la fenetre
	 * @param prof profondeur actuelle
	 * @return valeur maximum estimé par l'heuristique du plateau pour le joueur
	 */
	public int negasct(Plateau p,int alpha,int beta,int prof) {
		int betaAux,val;
		String coup = null;
		this.nb++;
		if(prof == this.profMax || p.finDePartie()){
			try {
				return this.h.eval(p,this.joueurMax,4);
			} catch (Exception e) {
				return 0;
			}
		}
		betaAux = beta;
		ArrayList<String> lst = p.coupsPossibles(this.joueurMax);
		Iterator<String> itr = lst.iterator();
		while (itr.hasNext()) {
			coup = itr.next();
		    Plateau nouveauPlateau = p.copy();
		    nouveauPlateau.play(coup,this.joueurMax); 
			val = -negasct(nouveauPlateau,-betaAux,-alpha,prof+1);
			if (val > alpha && val < beta && !nouveauPlateau.finDePartie() && lst.indexOf(coup) == 0) {
				alpha = negasct(nouveauPlateau,-beta,-val,prof+1);
			}
			alpha = Math.max(alpha, val);
			if(alpha >= beta) {
				return alpha;
			}
				
			betaAux = alpha + 1;
		}
		return alpha;
	}
}
