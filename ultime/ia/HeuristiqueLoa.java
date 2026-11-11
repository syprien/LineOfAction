package ultime.ia;

import java.util.ArrayList;
import java.util.Iterator;

import ultime.moteur.Couple;
import ultime.moteur.Plateau;

/**
 * Cette classe regrouper tout les fonctions qui compose l'heuristique à la base de
 * l'ia de notre jeu de LOA
 * 
 * @author tazzzz-syprien
 *
 */
public class HeuristiqueLoa{
	static final private byte NOIR = 1;
    static final private byte BLANC = -1;
    static final private char PION_NOIR = 'X';
    static final private char PION_BLANC = 'O';
    static final private byte DIMENSION = 8;
    // CORRECTIF (2026) : score d'une position gagnée. C'était Integer.MAX_VALUE, voir ligne_max().
    static final public int VICTOIRE = 10000;
    private byte ami;
    private byte enemi;
    
		/**
		 * Fonction evaluant un plateau du jeu LOA
		 * @param p plateau à evaluer
		 * @param joueur identifiant du joueur
		 * @param niveau niveau d'heuristique a applique
		 * @return
		 */
    
    	/*
    	 * L'argument niveau a été implémenter pour réaliser des tests internes de l'ia contre
    	 * elle-même
    	 */
		public int eval(Plateau p,String joueur,int niveau) {
			int res = 0,i=0,j=0;
			// CORRECTIF (2026) : == compare les références des String, pas le texte, et la recherche passe
			// de toute façon "blanc" en minuscules. Ce test était TOUJOURS faux : l'IA évaluait
			// toujours le plateau comme si elle était Noire, même quand elle jouait Blanc.
			if(joueur.equalsIgnoreCase("blanc")) {
				this.ami = BLANC;
				this.enemi = NOIR;
			} else {
				this.ami = NOIR;
				this.enemi = BLANC;
			}
			// CORRECTIF (2026) : NegaMax / NegaScout supposent une évaluation À SOMME NULLE :
			//     eval(p, "noir") == - eval(p, "blanc")
			// car ils calculent le score de l'adversaire et se contentent d'y mettre un moins.
			// Chaque critère doit donc être calculé pour LES DEUX camps et soustrait
			// (le mien - le sien), comme l'étaient déjà ligne_max et evaluation. centre() et
			// nbPion() n'étaient comptés que pour "ami" : vue de l'autre camp, la
			// recherche récompensait l'IA quand elle PERDAIT ses propres pions.
			// (Sans effet en 2008, car la recherche n'alternait jamais les joueurs.)
			switch (niveau) {
				case 4 :
					// CORRECTIF (2026) : nb_voisin() peut renvoyer 0 (aucun voisin) -> division par zéro.
					// Cette ArithmeticException était avalée en silence par NegaScout (score 0).
					res = (ligne_max(p,this.ami) - ligne_max(p,this.enemi))
					- evaluation(p,this.ami) + evaluation(p,this.enemi)
					+ centre(p,this.ami) - centre(p,this.enemi)
					+ (nbPion(p,this.ami) / Math.max(1, this.nb_voisin(p,this.ami)))
					- (nbPion(p,this.enemi) / Math.max(1, this.nb_voisin(p,this.enemi)));
					// CORRECTIF (2026) : "break" manquant. Sans lui, l'exécution « tombe » dans le
					// case 3, qui ÉCRASE res : le niveau 4 était en fait identique au niveau 3.
					break;
				case 3 :
					res = (ligne_max(p,this.ami) - ligne_max(p,this.enemi))
					- evaluation(p,this.ami) + evaluation(p,this.enemi)
					+ centre(p,this.ami) - centre(p,this.enemi); // CORRECTIF (2026) : somme nulle
				break;
				case 2 : 
					res = (ligne_max(p,this.ami) - ligne_max(p,this.enemi)) 
					- evaluation(p,this.ami) + evaluation(p,this.enemi);
					break;
				case 1 : 
					res = (ligne_max(p,this.ami) - ligne_max(p,this.enemi));
				break;
				default :
					res = (ligne_max(p,this.ami) - ligne_max(p,this.enemi))
					- evaluation(p,this.ami) + evaluation(p,this.enemi)
					+ centre(p,this.ami) - centre(p,this.enemi); // CORRECTIF (2026) : somme nulle
			}
			
			return res;
			
		}
		
		
		/**
		 * Fonction permettant de trouvé la chaine la plus longue formé par des pions du joueur
		 * @param p plateau
		 * @param couleur identifiant de la couleur des pions du joueur
		 * @return la taille de la chaine la plus longue ou un très grande valeur si la chaine comporte tous les pions du joueur
		 */
		private int ligne_max(Plateau p,int couleur) {
			boolean dejaParcouru [][] = new boolean[this.DIMENSION][this.DIMENSION];
			for(int i=0;i<this.DIMENSION;i++) {
				for(int j=0;j<this.DIMENSION;j++) {
					dejaParcouru [i][j]=false;
				}
			}
			
			int temp = 0;
			// CORRECTIF (2026) : partir de 0 au lieu de Integer.MIN_VALUE (si une couleur n'avait plus de pion,
			// MIN_VALUE était renvoyé et "a - b" dans eval() débordait).
			int max = 0;
			
			for(int k=0;k<this.DIMENSION;k++) {
				for(int l=0;l<this.DIMENSION;l++) {
					if(p.getTab()[k][l]==couleur && !dejaParcouru [k][l]) {
						temp = parcoursChemin(k,l,p,dejaParcouru);
						if(temp > max) {
							max = temp;
						}
					}
				}
			}
			if (liste_pion(p,couleur).size() == max) {
				// CORRECTIF (2026) : c'était Integer.MAX_VALUE. eval() y AJOUTE ensuite d'autres termes, et
				// Integer.MAX_VALUE + 1 repart à Integer.MIN_VALUE (dépassement d'entier) :
				// une position GAGNANTE pouvait être notée comme la pire possible.
				// Une constante grande mais sûre évite le débordement.
				max = VICTOIRE;
			}
			return max;
		}
		
	    private int parcoursChemin(int i, int j,Plateau p,boolean [][] dv) {
	    	ArrayList<Couple> visites = new ArrayList<Couple>();
	    	parcoursProfondeur(i,j,visites,p,dv);
	    	return visites.size();
		}


	    /**
	     * Fonction de parcours du plateau en partant d'un pion. On parcours le plateau comme si on explorait un graphe
	     * @param i ligne où se situe le pion de départ
	     * @param j colonne où se situe le pion de départ
	     * @param sommetsVisites ArrayList de Couple servant à représenter les sommets visités lors du parcours
	     */
	    private void parcoursProfondeur(int i,int j, 
	    		ArrayList<Couple> sommetsVisites,Plateau p,boolean [][] dv) {
	    	Couple s = new Couple(i,j);
	    	dv[i][j] = true;
	    	sommetsVisites.add(s);
	    	ArrayList<Couple> al = voisin(i,j,p);
	    	Iterator<Couple> it = al.iterator();
	    	while (it.hasNext()) { 
	    		Couple suivant = (Couple)it.next();
	    		if (!sommetsVisites.contains(suivant)) {
	    			parcoursProfondeur(suivant.getI(),suivant.getJ(), sommetsVisites,p,dv);
	    		}
	    	}
	    }
	    
	    /**
	     * Cette fonction permet d'obtenir une liste des pions adjacents à celui se trouvant 
	     * en (i,j) et de la même couleur que celui-ci.
	     * @param i ligne
	     * @param j colonne
	     * @return une liste des pions voisins du mï¿½me joueur
	     */
	    private ArrayList<Couple> voisin(int i,int j,Plateau p){
	    	ArrayList<Couple> res = new ArrayList<Couple>();
	    	if(i+1<DIMENSION && i+1>=0 && j+1<DIMENSION && j+1>=0)
	    	{
	    		if(p.getTab()[i][j]==p.getTab()[i+1][j+1]) {
	    			Couple c = new Couple(i+1,j+1);
	    			res.add(c);
	    		}
	    	}
	    	if(i<DIMENSION && i>=0 && j+1<DIMENSION && j+1>=0)
	    	{
	    		if(p.getTab()[i][j]==p.getTab()[i][j+1]) {
	    			Couple c = new Couple(i,j+1);
	    			res.add(c);
	    		}
	    	}
	    	if(i-1<DIMENSION && i-1>=0 && j+1<DIMENSION && j+1>=0)
	    	{
	    		if(p.getTab()[i][j]==p.getTab()[i-1][j+1]) {
	    			Couple c = new Couple(i-1,j+1);
	    			res.add(c);
	    		}
	    	}
	    	if(i+1<DIMENSION && i+1>=0 && j<DIMENSION && j>=0)
	    	{
	    		if(p.getTab()[i][j]==p.getTab()[i+1][j]) {
	    			Couple c = new Couple(i+1,j);
	    			res.add(c);
	    		}
	    	}
	    	if(i+1<DIMENSION && i+1>=0 && j-1<DIMENSION && j-1>=0)
	    	{
	    		if(p.getTab()[i][j]==p.getTab()[i+1][j-1]) {
	    			Couple c = new Couple(i+1,j-1);
	    			res.add(c);
	    		}
	    	}
	    	if(i-1<DIMENSION && i-1>=0 && j-1<DIMENSION && j-1>=0)
	    	{
	    		if(p.getTab()[i][j]==p.getTab()[i-1][j-1]) {
	    			Couple c = new Couple(i-1,j-1);
	    			res.add(c);
	    		}
	    	}
	    	if(i<DIMENSION && i>=0 && j-1<DIMENSION && j-1>=0)
	    	{
	    		if(p.getTab()[i][j]==p.getTab()[i][j-1]) {
	    			Couple c = new Couple(i,j-1);
	    			res.add(c);
	    		}
	    	}
	    	if(i-1<DIMENSION && i-1>=0 && j<DIMENSION && j>=0)
	    	{
	    		if(p.getTab()[i][j]==p.getTab()[i-1][j]) {
	    			Couple c = new Couple(i-1,j);
	    			res.add(c);
	    		}
	    	}
	    	return res;
	    }
	    
	    /* ********************************************************************************************** */
	    
	    /**
	        * Liste les pions d'une couleur
	        * @param p le plateau
	        * @param couleur
	        * @return liste des pions
	        */
	       private ArrayList<Couple> liste_pion(Plateau p, int couleur) {
	               ArrayList<Couple> tmp = new ArrayList<Couple>();
	               for (int i = 0; i < this.DIMENSION; i++) {
	                       for (int j = 0; j < this.DIMENSION; j++) {
	                               if (p.getTab()[i][j] == couleur) {
	                                       tmp.add(new Couple(i, j));
	                               }
	                       }
	               }
	               return tmp;
	       }

	       /**
	        * Distance entre deux piï¿½ces en nombre de case
	        * @param a une piï¿½ce
	        * @param b une autre piï¿½ce
	        * @return int - la distance
	        */
	       private int dist(Couple a, Couple b) {
	               int x = Math.abs(a.getI() - b.getI());
	               int y = Math.abs(a.getJ() - b.getJ());
	               if (x > y) {
	                       return x;
	               } else {
	                       return y;
	               }
	       }

	       /**
	        * Trouve le barycentre de la formation des pions
	        * @param la liste des pièces
	        * @return Couple - le barycentre
	        */
	       private Couple barycentre(ArrayList<Couple> p) {
               int x = 0, y = 0;
               Iterator<Couple> it = p.iterator();
               Couple aux;
               while (it.hasNext()) {
            	   aux = it.next();
                   x += aux.getI();
                   y += aux.getJ();
               }
               if(p.size() != 0) {
            	   // CORRECTIF (2026) : x / p.size() est une division ENTIÈRE : elle est tronquée AVANT
            	   // l'appel à Math.round(), donc l'arrondi ne servait à rien (3,9 devenait 3, pas 4).
            	   // Convertir d'abord en float pour que la division garde ses décimales.
            	   x = Math.round((float) x / p.size());
            	   y = Math.round((float) y / p.size());
               }

               return new Couple(x, y);
       }

	       /**
	        * Evalue un plateau de jeu
	        * @param le plateau
	        * @param couleur
	        * @return int - heuristique
	        */
	       public int evaluation(Plateau p, int couleur) {
	               int res = 0;
	               ArrayList<Couple> liste = liste_pion(p, couleur);
	               Couple bary = barycentre(liste);
	               Iterator<Couple> it = liste.iterator();
	               while (it.hasNext()) {
	                       res += dist(it.next(), bary);
	               }

	               return mouv_min(res, liste.size());
	       }

	       /**
	        * retire les distances normales pour former un bloc (les pièces ne
	peuvent se chevaucher)
	        * @param res le résultat de l'heuristique
	        * @param pion le nombre de pion
	        * @return heuristique finale
	        */
	       private int mouv_min(int res, int pion) {
	               if (pion == 1) {
	                       return res;
	               } else if (pion <= 9) {
	                       return res - pion;
	               } else if (pion > 9) {
	                       return res - 8 - (2 * (pion - 8));
	               }
	               return res;
	       }
	       
	       private int centre(Plateau p, int couleur) {
	    	   int res = 0;
	    	   int pion = 0;
	    	   for (int i = 0; i < this.DIMENSION; i++) {
                   for (int j = 0; j < this.DIMENSION; j++) {
                           if (p.getTab()[i][j] == couleur) {
                             res += valeur[j*8+i];
                             pion++;
                           }
                   }
           }
			// CORRECTIF (2026) : éviter une division par zéro si la couleur n'a plus de pion.
			return (pion == 0) ? 0 : res/pion;
	       }
	       
	       private int[] valeur = {	-16,-8,-4,-4, -4,-4,-8,-16,
	    		   					-8, 1, 1,  1,  1,  1, 1,-8,
	    		   					-2, 1, 6,  6,  6,  6, 1,-4,
	    		   					-2, 1, 6, 12, 12,  6, 1,-4,
	    		   					-2, 1, 6, 12, 12,  6, 1,-4,
	    		   					-2, 1, 6,  6,  6,  6, 1,-4,
	    		   					-8, 1, 1,  1,  1,  1, 1,-8,
	    		   					-16,-8,-4,-4, -4,-4,-8,-16,
	       							};
	       
	       
	     /**
	     * Evalue le plateau en fonction du nombre de pion du joueur et de celui de l'aversaire
	     * @param p
	     * @param couleur
	     * @return
	     */
	    private int nbPion(Plateau p,int couleur) {
	    	   int amis = 0 , enemie =0;
	    	   for(int i=0;i<DIMENSION;i++)
	    		   for(int j=0;j<DIMENSION;j++) {
	    			   // CORRECTIF (2026) : utiliser le paramètre "couleur" (il était ignoré, this.ami était
	    			   // toujours utilisé) pour pouvoir appeler la fonction pour les deux camps.
	    			   // NOIR = 1 et BLANC = -1, donc l'autre couleur est simplement -couleur.
	    			   if(p.getTab()[i][j] == couleur)
	    			   	amis++;
	    			   // CORRECTIF (2026) : un simple "else" comptait aussi les cases VIDES (valeur 0) comme
	    			   // des pions ennemis (une cinquantaine !). Tester explicitement la couleur ennemie.
	    			   else if(p.getTab()[i][j] == -couleur)
	    			   	enemie++;
	    		   }
	    	   
	    	   return ((enemie*enemie) - amis);
	    	   
	       }
	       
	       private int nb_voisin (Plateau p,int couleur ){
	    	   
	    	   int res = 0;
               ArrayList<Couple> liste = liste_pion(p, couleur);
               Iterator<Couple> ite= liste.iterator();
               while (ite.hasNext()) {
                      // CORRECTIF (2026) : chaque appel à ite.next() FAIT AVANCER l'itérateur. L'appeler deux fois
                      // prenait la ligne d'un pion et la colonne du SUIVANT, sautait la moitié
                      // des pions, et levait NoSuchElementException quand leur nombre était impair.
                      // Appeler next() une seule fois et garder le résultat dans une variable.
                      Couple pion = ite.next();
                      res += voisin(pion.getI(),pion.getJ(),p).size();
               }
			// CORRECTIF (2026) : éviter une division par zéro (Math.round sur une division entière ne servait à rien).
			return (liste.size() == 0) ? 0 : res / liste.size();
	       }
	       
}
