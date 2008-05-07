package jeux.alg;

import java.util.ArrayList;
import java.util.Iterator;

import moteur.Plateau;

public class NegaScout {
	private final static int PROFMAXDEFAUT = 3;
	private int profMax = PROFMAXDEFAUT;
	
	private Heuristique h;
	private String joueurMin;
	private String joueurMax;

	/* constructeurs */
	
	public NegaScout(Heuristique h,String joueurMax, String joueurMin){
		this.h = h;
		this.joueurMin = joueurMin;
		this.joueurMax = joueurMax;
//		System.out.println("Initialisation d'un MiniMax de profondeur " + profMax);
	}

	public NegaScout(Heuristique h, String joueurMax, String joueurMin, int profMaxi){
		this.h = h;
		this.joueurMin = joueurMin;
		this.joueurMax = joueurMax;
		profMax = profMaxi;
//		System.out.println("Initialisation d'un MiniMax de profondeur " + profMax);
	}

	public String toString() {
		return "NegaScout";
	}
	
	public String meilleurCoup(Plateau p, String j) {
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
		      aux = negasct(nouveauPlateau,Integer.MIN_VALUE,Integer.MAX_VALUE,this.profMax,"BLANC");
		      if(meilleur < aux) {
		    	  meilleur = aux;
		    	  meilleurCoup = coup; 
		      }
		}
		
		return meilleurCoup;
	}
	
	public int negasct(Plateau p,int alpha,int beta,int prof,String j) {
		int betaAux,val;
		String coup = null;
		if(prof == this.profMax || p.finDePartie()){
			return this.h.eval(p);
		}
		betaAux = beta;
		ArrayList<String> lst = p.coupsPossibles(j);
		Iterator<String> itr = lst.iterator();
		while (itr.hasNext()) {
			coup = itr.next();
		    Plateau nouveauPlateau = p.copy();
		    nouveauPlateau.play(coup,this.joueurMax);
			val = -negasct(nouveauPlateau,-betaAux,-alpha,prof+1,j);
			if (val > alpha && val < beta && !nouveauPlateau.finDePartie() && lst.indexOf(coup) == 0) {
				alpha = negasct(nouveauPlateau,-beta,-val,prof+1,j);
			}
			alpha = Math.max(alpha, val);
			if(alpha >= beta)
				return alpha;
			betaAux = alpha + 1;
		}
		return alpha;
	}
}
