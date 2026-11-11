package jeux.alg;

import ia.HeuristiqueLoa;

import java.util.ArrayList;
import java.util.Iterator;

import moteur.Plateau;
/**
 * Fonction d'implementation de l'agorithme negamax aplique a alpha-beta
 * 
 * @author tazzzz-syprien
 * 
 *
 */
public class NegaAB {
		private final static int PROFMAXDEFAUT = 3;
		private int profMax = PROFMAXDEFAUT;
		// CORRECTIF (2026) : remplace Integer.MIN_VALUE / MAX_VALUE, voir le commentaire dans NegaScout.
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
		public NegaAB(HeuristiqueLoa h,String joueurMax){
			this.h = h;
			this.joueurMax = joueurMax.toLowerCase(); // CORRECTIF (2026) : les identifiants de couleur sont comparés en minuscules
			this.nb = 0;
		}
		/**
		 * constructeur de la classe NegaAB
		 * @param h heuristique utilisé
		 * @param joueurMax joueur dont on souhaite maximisé le score
		 * @param profMaxi horizon limite de l'algorithme 
		 */
		public NegaAB(HeuristiqueLoa h, String joueurMax,int profMaxi){
			this.h = h;
			this.joueurMax = joueurMax.toLowerCase(); // CORRECTIF (2026) : les identifiants de couleur sont comparés en minuscules
			this.profMax = profMaxi;
			this.nb = 0;
		}

		public String toString() {
			return "NegaAB";
		}
		
		/**
		 * Cette fonction retourne le meilleur coup possible estimé pour le joueur j
		 * @param p plateau de jeu
		 * @return meilleur coup possible estimé pour le joueur j
		 */
		public String meilleurCoup(Plateau p) {
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
			      // CORRECTIF (2026) : après notre coup c'est à l'adversaire, et son score est négativé (voir NegaScout).
			      aux = -nAB(nouveauPlateau,-INFINI,-meilleur,0,adversaire(this.joueurMax));
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
		public int nAB(Plateau p,int alpha,int beta,int prof,String joueur) {
			// CORRECTIF (2026) : mêmes bugs que dans NegaScout (voir ses commentaires) : le joueur
			// doit alterner (paramètre joueur) et les feuilles sont évaluées pour le joueur
			// qui a le trait.
			String coup;
			int val;
			if(prof == this.profMax || p.finDePartie()){
				return this.h.eval(p,joueur,3);
			}
			ArrayList<String> lst = p.coupsPossibles(joueur);
			if(lst.size() == 0) {
				return this.h.eval(p,joueur,3);
			}
			Iterator<String> itr = lst.iterator();
			while (itr.hasNext()) {
				coup = itr.next();
				Plateau nouveauPlateau = p.copy();
				nouveauPlateau.play(coup,joueur);
				val = -nAB(nouveauPlateau,-beta,-alpha,prof+1,adversaire(joueur));
				if (val > alpha) {
					alpha = val;
					// CORRECTIF (2026) : la condition de coupure est alpha >= beta. Avec ">", un nœud
					// dont la valeur valait exactement beta continuait d'explorer pour rien.
					if(alpha >= beta)
						return alpha;
				}
			}
			return alpha;
		}

		// CORRECTIF (2026) : fonction utilitaire pour alterner le joueur à chaque niveau de l'arbre.
		private static String adversaire(String joueur) {
			return joueur.equalsIgnoreCase("noir") ? "blanc" : "noir";
		}

}
