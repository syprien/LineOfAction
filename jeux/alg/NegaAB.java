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
			this.joueurMax = joueurMax;
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
			this.joueurMax = joueurMax;
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
			int meilleur = Integer.MIN_VALUE;
			
			int aux;
			String coup = null,meilleurCoup = null;
			Iterator<String> itr = lst.iterator();
			while (itr.hasNext()) {
			      coup = itr.next();
			      Plateau nouveauPlateau = p.copy();
			      nouveauPlateau.play(coup,this.joueurMax);
			      aux = nAB(nouveauPlateau,Integer.MIN_VALUE,Integer.MAX_VALUE,0);
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
		public int nAB(Plateau p,int alpha,int beta,int prof) {
			String coup;
			int val;
			if(prof == this.profMax || p.finDePartie()){
				return this.h.eval(p,this.joueurMax,3);
			}
			ArrayList<String> lst = p.coupsPossibles(this.joueurMax);
			Iterator<String> itr = lst.iterator();
			while (itr.hasNext()) {
				coup = itr.next();
			    Plateau nouveauPlateau = p.copy();
			    nouveauPlateau.play(coup,this.joueurMax); 
			    val = -nAB(nouveauPlateau,-beta,-alpha,prof+1);
			    if (val > alpha) {
			    	alpha = val;
			    	if(alpha > beta)
			    		return alpha;
			    }
			}
			return alpha;
		}

}
