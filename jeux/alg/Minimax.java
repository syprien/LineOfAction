package jeux.alg;  

import java.util.ArrayList;
import java.util.Iterator;

import moteur.Plateau;
/**
 * Fonction d'implementation de l'agorithme Minimax basique
 * 
 * @author tazzzz-syprien
 * 
 */

/* Cette fonction nous a servi pour tester les premières heuristiques 
 * mais nous l'avons vite abandonné car le developpement de toutes les
 * possibilités était beaucoup trop long.
 */

/**
 * @author tazzzz
 *
 */
public class Minimax {

	private final static int PROFMAXDEFAUT = 3;
	private int profMax = PROFMAXDEFAUT;

	private int nbnoeuds; 
	private int nbfeuilles;
	private Heuristique h;
	private String joueurMin;
	private String joueurMax;

	/* constructeurs */

	/**
	 * constructeur de la classe Minimax
	 * @param h heuristique utilisé
	 * @param joueurMax joueur dont on souhaite maximisé le score
	 * @param joueurMin joueur dont on souhaite minimisé le score
	 */
	public Minimax(Heuristique h,String joueurMax, String joueurMin){
		this.h = h;
		this.joueurMin = joueurMin;
		this.joueurMax = joueurMax;		
//		System.out.println("Initialisation d'un MiniMax de profondeur " + profMax);
	}

	/**
	 * constructeur de la classe Minimax avec choix de la profondeur (de l'horizon maximum)
	 * @param h heuristique utilisé
	 * @param joueurMax joueur dont on souhaite maximisé le score
	 * @param joueurMin joueur dont on souhaite minimisé le score
	 * @param profMaxi horizon limite de l'algorithme
	 */
	public Minimax(Heuristique h, String joueurMax, String joueurMin, int profMaxi){
		this.h = h;
		this.joueurMin = joueurMin;
		this.joueurMax = joueurMax;
		profMax = profMaxi;
		
//		System.out.println("Initialisation d'un MiniMax de profondeur " + profMax);
	}

	public String toString() {
		return "MiniMax";
	}
	
	/**
	 * maximisation du score de l'ami
	 * @param p plateau de jeu
	 * @param profondeur
	 * @param nb valeur de controle
	 * @return valeur maximum estimé par l'heuristique du plateau pour le joueur ami
	 */
	private int MaxMin(Plateau p,int profondeur,int nb) {
		ArrayList<String> lst;
	    // CORRECTIF (2026) : "nb" est un paramètre int, et Java passe les types primitifs PAR VALEUR :
	    // nb++ ne modifiait qu'une copie locale, donc le nombre de nœuds affiché valait toujours 0.
	    // On compte dans un attribut à la place (nbnoeuds était déclaré mais jamais utilisé).
	    this.nbnoeuds++;
		if(profondeur == this.profMax) 
			return this.h.eval(p);
		lst = p.coupsPossibles(this.joueurMax);	
		if(lst.size() == 0 ) {
			return this.h.eval(p);
		}
		int meilleur = Integer.MIN_VALUE;
		
		Iterator<String> itr = lst.iterator();
		while (itr.hasNext()) {
				
		      String c = itr.next();
		      Plateau nouveauPlateau = p.copy();
		      nouveauPlateau.play(c,this.joueurMax);
		      meilleur = Math.max(meilleur, MinMax(nouveauPlateau,profondeur+1,nb));
		}
		return meilleur;
	}
	
	/**
	 * minimisation du score de l'enemi
	 * @param p plateau de jeu
	 * @param profondeur
	 * @param nb valeur de controle
	 * @return valeur minimum estimé par l'heuristique du plateau pour le joueur enemi
	 */
	private int MinMax(Plateau p,int profondeur,int nb) {
		ArrayList<String> lst;
		// CORRECTIF (2026) : "nb" est un paramètre int, et Java passe les types primitifs PAR VALEUR :
		// nb++ ne modifiait qu'une copie locale, donc le nombre de nœuds affiché valait toujours 0.
		// On compte dans un attribut à la place (nbnoeuds était déclaré mais jamais utilisé).
		this.nbnoeuds++;
		if(profondeur == this.profMax) { 
			return this.h.eval(p);
		}
		lst = p.coupsPossibles(this.joueurMin);	
		if(lst.size() == 0 ) {
			return this.h.eval(p);
		}
		int pire = Integer.MAX_VALUE;
		Iterator<String> itr = lst.iterator();
		while (itr.hasNext()) {
		      String c = itr.next();
		      Plateau nouveauPlateau = p.copy();
		      nouveauPlateau.play(c,this.joueurMin);
		      pire = Math.min(pire, MaxMin(nouveauPlateau,profondeur+1,nb));
		}
		return pire;
	}
	
	/**
	 * Cette fonction retourne le meilleur coup possible estimé pour le joueur j
	 * @param p plateau de jeu
	 * @param j couleur du joueur
	 * @return meilleur coup possible estimé pour le joueur j
	 */
	public String meilleurCoup(Plateau p, String j) {
		int nbNoeud = 0;
		this.nbnoeuds = 0; // CORRECTIF (2026) : remise à zéro du compteur pour cette recherche
		ArrayList<String> lst = p.coupsPossibles(j);
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
		     
		      aux = MinMax(nouveauPlateau,0,nbNoeud);
		      if(meilleur < aux) {
		    	  meilleur = aux;
		    	  meilleurCoup = coup; 
		      }
		}
		
		System.out.println("nombre de noeud exploré : " + this.nbnoeuds); // CORRECTIF (2026) : afficher le vrai compteur
		return meilleurCoup;
	}
	
}