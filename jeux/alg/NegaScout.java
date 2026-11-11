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
	// CORRECTIF (2026) : « l'infini » de la fenêtre alpha-bêta. C'était Integer.MIN_VALUE
	// et Integer.MAX_VALUE, mais negamax PREND L'OPPOSÉ des bornes à chaque niveau, et en Java
	// -Integer.MIN_VALUE == Integer.MIN_VALUE (débordement : +2147483648 ne tient pas dans un
	// int). La fenêtre était faussée en silence. Toute valeur supérieure à tous les scores convient.
	private final static int INFINI = 1000000;
	
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
		// CORRECTIF (2026) : le constructeur à 2 arguments passait la couleur en minuscules, pas celui-ci.
		this.joueurMax = joueurMax.toLowerCase();
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
		int meilleur = -INFINI; // CORRECTIF (2026) : voir INFINI
		
		int aux;
		String coup = null,meilleurCoup = null;
		Iterator<String> itr = lst.iterator();
		while (itr.hasNext()) {
		      coup = itr.next();
		      Plateau nouveauPlateau = p.copy();
		      nouveauPlateau.play(coup,this.joueurMax);
		      // CORRECTIF (2026) : deux bugs sur cette ligne.
		      // 1) Après NOTRE coup, c'est à l'ADVERSAIRE de jouer : passer adversaire(joueurMax).
		      // 2) negasct() renvoie le score du joueur QUI A LE TRAIT (ici l'adversaire).
		      //    Ce qui est bon pour lui est mauvais pour nous : notre score est MOINS le sien.
		      // (Amélioration : prendre -meilleur comme beta permet aussi d'élaguer à la racine.)
		      aux = -negasct(nouveauPlateau,-INFINI,-meilleur,0,adversaire(this.joueurMax));
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
	public int negasct(Plateau p,int alpha,int beta,int prof,String joueur) {
		// CORRECTIF (2026) : nouveau paramètre "joueur" = le joueur qui a le trait à ce nœud.
		// La version de 2008 générait toujours les coups de joueurMax, à CHAQUE niveau :
		// l'adversaire ne jouait jamais dans l'arbre de recherche, l'IA planifiait comme si elle
		// pouvait jouer 3 ou 4 coups d'affilée. Negamax doit alterner les joueurs.
		int b,val;
		String coup = null;
		String autre = adversaire(joueur);
		this.nb++;
		if(prof == this.profMax || p.finDePartie()){
			// CORRECTIF (2026) : negamax a besoin de l'évaluation du point de vue du
			// joueur QUI A LE TRAIT (joueur), pas toujours joueurMax.
			// L'ancien "try { ... } catch (Exception e) { return 0; }" a été retiré : il
			// cachait les bugs de l'heuristique (division par zéro, NoSuchElementException...)
			// en notant ces positions 0 en silence. Ne jamais avaler d'exceptions ainsi.
			return this.h.eval(p,joueur,4);
		}
		ArrayList<String> lst = p.coupsPossibles(joueur);
		if(lst.size() == 0) {
			// CORRECTIF (2026) : aucun coup légal : évaluer au lieu de renvoyer alpha inchangé.
			return this.h.eval(p,joueur,4);
		}
		b = beta;
		boolean premier = true;
		Iterator<String> itr = lst.iterator();
		while (itr.hasNext()) {
			coup = itr.next();
			Plateau nouveauPlateau = p.copy();
			nouveauPlateau.play(coup,joueur);
			val = -negasct(nouveauPlateau,-b,-alpha,prof+1,autre);
			// CORRECTIF (2026) : NegaScout explore le PREMIER coup avec la fenêtre complète, puis
			// chaque AUTRE coup avec une « fenêtre nulle » (alpha, alpha+1) qui répond seulement à
			// « ce coup est-il meilleur qu'alpha ? ». Quand la réponse est oui
			// (alpha < val < beta), ce coup doit être RÉEXPLORÉ avec une vraie fenêtre pour
			// obtenir sa valeur exacte. Le code de 2008 faisait l'inverse : il ne réexplorait que
			// le premier coup (lst.indexOf(coup) == 0), dont la valeur était déjà exacte,
			// et gardait le résultat SANS le signe moins.
			// Un enfant qui est une feuille renvoie de toute façon une valeur exacte : pas de réexploration.
			if(!premier && val > alpha && val < beta && prof + 1 < this.profMax) {
				val = -negasct(nouveauPlateau,-beta,-val,prof+1,autre);
			}
			alpha = Math.max(alpha, val);
			if(alpha >= beta) {
				return alpha;
			}
			b = alpha + 1;
			premier = false;
		}
		return alpha;
	}

	// CORRECTIF (2026) : fonction utilitaire pour alterner le joueur à chaque niveau de l'arbre.
	private static String adversaire(String joueur) {
		return joueur.equalsIgnoreCase("noir") ? "blanc" : "noir";
	}
}
