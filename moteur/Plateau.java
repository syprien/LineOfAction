/**
 * Cette classe regroupe l'ensemble des éléments de gestion du plateau
 * @author Syprien - tazzzz
 * @version 1.0
 */
package moteur;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * Cette classe regroupe tout les fonctions ainsi que la structure de donnée nécessaire au fonctionnement
 * d'une partie de Line Of Action
 * 
 * @author tazzzz-syprien
 *
 */

public class Plateau implements Partie1 {
	
    static final private byte NOIR = 1;
    static final private byte BLANC = -1;
    static final private char PION_NOIR = 'X';
    static final private char PION_BLANC = 'O';
    static final private byte DIMENSION = 8;
   
    private byte[][] tab;
    private int nbPionNoir;
    private int nbPionBlanc;

    public Plateau() {
        this.tab = new byte[DIMENSION][DIMENSION];
        for(int i = 0 ;i<DIMENSION; i++) {
			for(int j = 0 ;j<DIMENSION; j++) {
				this.tab[i][j]=0;
			}
        }
        for(int i=1;i<7;i++) {
        	this.tab[0][i] = BLANC;
        	this.tab[7][i] = BLANC;
        	this.tab[i][0] = NOIR;
        	this.tab[i][7] = NOIR;
        }
        this.nbPionBlanc=12;
        this.nbPionNoir=12;
    }
    

    @Override
    public boolean estValide(String move, String player) {
        // TODO Syprien : Valider le coup

        Couple[] a = trad(move);

        int x = a[0].getI() - a[1].getI();
        int y = a[0].getJ() - a[1].getJ();

        int direction = direction(a[0], a[1]);

        int c = 0;
        /* on test si le pion est bien de la couleur du joueur */
        if ((this.tab[a[0].getI()][a[0].getJ()] == BLANC && player.compareToIgnoreCase("noir") == 0) ||
        		(this.tab[a[0].getI()][a[0].getJ()] == NOIR && player.compareToIgnoreCase("blanc") == 0) ||
        		// CORRECTIF (2026) : une case de départ VIDE était aussi acceptée. L'IA ne le demandait
        		// jamais, mais estValide() doit la refuser d'elle-même.
        		(this.tab[a[0].getI()][a[0].getJ()] == 0))
        	return false;
        /* on test si c'est pas hors du plateau ou si le mouvement ne deplace pas le pion*/
        if(a[0].getI()<0 || a[1].getI()<0 || a[0].getJ()>=DIMENSION || a[1].getJ()>=DIMENSION 
        		|| a[0].getJ()<0 || a[1].getJ()<0 || (x == 0 && y ==0)){
        	return false;
        }
        /* on test si la case d'arrive n'est pas déja occupé par un pion de la couleur du joueur */
        if(this.tab[a[0].getI()][a[0].getJ()] == this.tab[a[1].getI()][a[1].getJ()]) {
        	return false;
        }
        // Horizontale
        if (direction == 7 || direction == 3) {
            c = compteHorizonztale(a[0].getJ());

            if (c != Math.abs(x)) {
                return false;
            } else {
                if (direction == 3) {
                    for (int f = a[0].getI(); f < a[1].getI(); f++) {
                        if (tab[f][a[0].getJ()] != tab[a[0].getI()][a[0].getJ()] && tab[f][a[0].getJ()] != 0) {
                            return false;
                        }
                        /*if(tab[f][a[0].getJ()] == tab[a[0].getI()][a[0].getJ()] && a[0].getI()!=f 
                        		&& tab[f-1][a[0].getJ()]!= 0){
                        	return false;
                        }*/
                    }
                    return true;
                } else {
                    for (int f = a[0].getI(); f > a[1].getI(); f--) {
                        if (tab[f][a[0].getJ()] != tab[a[0].getI()][a[0].getJ()] && tab[f][a[0].getJ()] != 0) {
                            return false;
                        }
                        /*if(tab[f][a[0].getJ()] == tab[a[0].getI()][a[0].getJ()] && a[0].getI()!=f 
                        		&& tab[f+1][a[0].getJ()]!= 0){
                        	return false;
                        }*/
                    }
                    return true;
                }
            }

            // Verticale
        } else if (direction == 1 || direction == 5) {
            c = compteVerticale(a[0].getI());
            if (c != Math.abs(y)) {
                return false;
            } else {
                if (direction == 1) {
                    for (int f = a[0].getJ(); f < a[1].getJ(); f++) {
                        if (tab[a[0].getI()][f] != tab[a[0].getI()][a[0].getJ()] && tab[a[0].getI()][f]!= 0) {
                            return false;
                        }
                        /*if(tab[a[0].getI()][f] == tab[a[0].getI()][a[0].getJ()] && a[0].getJ()!=f 
                        		&& tab[a[0].getI()][f-1]!= 0){
                        	return false;
                        }*/
                    }
                    return true;
                } else {
                    for (int f = a[0].getJ(); f > a[1].getJ(); f--) {
                        if (tab[a[0].getI()][f] != tab[a[0].getI()][a[0].getJ()] && tab[a[0].getI()][f]!= 0) {
                            return false;
                        }
                        /*if(tab[a[0].getI()][f] == tab[a[0].getI()][a[0].getJ()] && a[0].getJ()!=f 
                        		&& tab[a[0].getI()][f+1]!= 0){
                        	return false;
                        }*/
                    }
                    return true;
                }
            }

            // Antislash
        } else if (direction == 8 || direction == 4) {

            // x et y de signes différents donc diagonale haut-gauche /
            // bas-droit
            if (x - y != 0) {
                c = compteAntiSlash(a[0].getI(), a[0].getJ());

                if (c != Math.abs(y) || c != Math.abs(x)) {
                    return false;
                } else {

                    if (direction == 4) {
                        // CORRECTIF (2026) : la version de 2008 utilisait deux boucles IMBRIQUÉES (f sur les lignes, g sur
                        // les colonnes), qui vérifient toutes les cases du RECTANGLE entre le départ et
                        // l'arrivée, pas seulement les cases SUR la diagonale. Un coup diagonal légal était
                        // refusé dès qu'un pion ennemi se trouvait n'importe où dans ce rectangle.
                        // Sur une diagonale, la ligne et la colonne avancent ENSEMBLE : une boucle, un compteur k.
                        for (int k = 1; k < c; k++) {
                            int f = a[0].getI() + k;
                            int g = a[0].getJ() - k;
                            if (tab[f][g] != tab[a[0].getI()][a[0].getJ()] && tab[f][g] != 0) {
                                return false;
                            }
                        }
                        return true;
                    } else {
                        // CORRECTIF (2026) : une seule boucle le long de la diagonale (voir le premier cas diagonal plus haut).
                        for (int k = 1; k < c; k++) {
                            int f = a[0].getI() - k;
                            int g = a[0].getJ() + k;
                            if (tab[f][g] != tab[a[0].getI()][a[0].getJ()] && tab[f][g] != 0) {
                                return false;
                            }
                        }
                        return true;
                    }
                }

            }
        } else if (direction == 2 || direction == 6) {
            // slash
            c = compteSlash(a[0].getI(), a[0].getJ());
	        if (c != Math.abs(x) || c != Math.abs(y)) {
	            return false;
	        } else {
	
	            if (direction == 2) {
	                // CORRECTIF (2026) : une seule boucle le long de la diagonale (voir le premier cas diagonal plus haut).
	                for (int k = 1; k < c; k++) {
	                	int f = a[0].getI() + k;
	                	int g = a[0].getJ() + k;
	                	if (tab[f][g] != tab[a[0].getI()][a[0].getJ()] && tab[f][g] != 0) {
	                		return false;
	                	}
	                }
	                return true;
	            } else {
	                // CORRECTIF (2026) : une seule boucle le long de la diagonale (voir le premier cas diagonal plus haut).
	                for (int k = 1; k < c; k++) {
	                	int f = a[0].getI() - k;
	                	int g = a[0].getJ() - k;
	                	if (tab[f][g] != tab[a[0].getI()][a[0].getJ()] && tab[f][g] != 0) {
	                		return false;
	                	}
	                }
	                return true;
	            }
	        }
        }
        return false;
    }

    private int direction(Couple d, Couple a) {

        int x = a.getI() - d.getI();
        int y = a.getJ() - d.getJ();

        if (x == 0 && y == 0) {
            return 0;
        }

        if (x == 0) {
            if (y > 0) {
                return 1;
            } else {
                return 5;
            }
        } else if (x > 0) {
            if (y > 0) {
                return 2;
            } else if (y == 0) {
                return 3;
            } else {
                return 4;
            }
        } else {
            if (y < 0) {
                return 6;
            } else if (y == 0) {
                return 7;
            } else {
                return 8;
            }
        }

    }

    private int compteHorizonztale(int j) {
        int c = 0;
        for (int i = 0; i < DIMENSION; i++) {
            if (tab[i][j] != 0) {
                c++;
            }
        }
        return c;

    }

    private int compteVerticale(int i) {
        int c = 0;
        for (int j = 0; j < DIMENSION; j++) {
            if (tab[i][j] != 0) {
                c++;
            }
        }
        return c;

    }
    // TODO tazzzz : Vérifier les bornes des whiles
    private int compteSlash(int i, int j) {
        int a = i-1;
        int b = j-1;

        int c = 1;
        while (a >= 0 && b >= 0) {
            if (tab[a][b] != 0) {
                c++;
            }
            a--;
            b--;
            
        }

        b = j+1;
        a = i+1;
        while (a < DIMENSION && b < DIMENSION) {
            if (tab[a][b] != 0) {
                c++;
            }
            a++;
            b++;
        }

        return c;

    }
    // TODO tazzzz : Vérifier les bornes des whiles
    private int compteAntiSlash(int i, int j) {

        int a = i;
        int b = j;

        int c = 0;

        while (a >= 0 && b < DIMENSION) {
            if (tab[a][b] != 0) {
                c++;
            }
            a--;
            b++;
        }
        b = j-1;
        a = i+1;
        while (a < DIMENSION && b >= 0) {
            if (tab[a][b] != 0) {
                c++;
            }
            a++;
            b--;
        }

        return c;

    }
    
    /**
     * Traduit un mouvement envoye par le serveur en deux objets de type Couple facilement
     * exploitable par le programme
     * @param place le mouvement envoyé par le serveur
     * @return une tableau de deux Couples
     */
    private Couple[] trad(String place) {

        Couple[] c = new Couple[2];

        c[0] = new Couple(tradChar(place.charAt(0)), Character.getNumericValue(place.charAt(1))-1);
        c[1] = new Couple(tradChar(place.charAt(3)), Character.getNumericValue(place.charAt(4))-1);
        return c;
    }

    /**
     * traduit le caractère donne pour un mouvement en entier pour le situé dans le plateau
     * @param c le caratère
     * @return la traduction en entier du caractère pour le plateau
     */
    private int tradChar(char c) {
        int i = 0;
        switch (c) {
        case 'A':
            i = 0;
            break;
        case 'B':
            i = 1;
            break;
        case 'C':
            i = 2;
            break;
        case 'D':
            i = 3;
            break;
        case 'E':
            i = 4;
            break;
        case 'F':
            i = 5;
            break;
        case 'G':
            i = 6;
            break;
        case 'H':
            i = 7;
            break;
        case 'Z':
        	i = -1;
        	break;
        default :
        	i = -2;
        }
        return i;
    }
    
    @Override
    public boolean finDePartie() {
    	int i,j;
    	boolean finNoir=false,
    			finBlanc=false;
    	// CORRECTIF (2026) : le compteur « ok » s'arrêtait après les DEUX premiers pions rencontrés, même
    	// s'ils étaient de la même couleur. Sur le plateau de départ, ce sont A2 et A3, deux pions blancs :
    	// seule la victoire de Blanc était détectée, celle de Noir jamais tant que la colonne A gardait au
    	// moins deux pions blancs. On retient plutôt si chaque couleur a déjà été testée.
    	boolean noirTeste=false, blancTeste=false;
    	i=j=0;
    	while (!(noirTeste && blancTeste) && (i<DIMENSION && j <DIMENSION) && !finNoir && !finBlanc){
    		if(this.tab[i][j] == NOIR && !noirTeste){
    			finNoir = parcoursChemin(i,j);
    			noirTeste = true;
    		}
    		if(this.tab[i][j] == BLANC && !blancTeste){
    			finBlanc = parcoursChemin(i,j);
    			blancTeste = true;
    		}
    		if(j==7){
    			i++;
    			j=0;
    		} else 
    			j++;
    	}
        return finNoir || finBlanc;
    }
    
    
    /**
     * Fonction de parcours d'un chemin de pion dans le plateau de jeu.
     * @param i ligne du 1er pion
     * @param j colonne du 1er pion
     * @return retourne un booleen qui indique s'il y a un chemin qui parcours tout les pions restant du joueur
     */
    public boolean parcoursChemin(int i, int j) {
    	ArrayList<Couple> visites = new ArrayList<Couple>();
    	parcoursProfondeur(i,j,visites);
    	if(this.tab[i][j]==NOIR && visites.size() == this.nbPionNoir){
    		return true;
    	}
    	if(this.tab[i][j]==BLANC && visites.size() == this.nbPionBlanc){
    		return true;
    	}
		return false;
	}


    /**
     * Fonction de parcours du plateau en partant d'un pion. On parcours le plateau comme si on explorait un graphe
     * @param i ligne où se situe le pion de départ
     * @param j colonne où se situe le pion de départ
     * @param sommetsVisites ArrayList de Couple servant à représenter les sommets visités lors du parcours
     */
    private void parcoursProfondeur(int i,int j, 
    		ArrayList<Couple> sommetsVisites) {
    	Couple s = new Couple(i,j);
    	sommetsVisites.add(s);
    	ArrayList<Couple> al = this.voisin(i,j);
    	Iterator<Couple> it = al.iterator();
    	while (it.hasNext()) { 
    		Couple suivant = (Couple)it.next();
    		if (!sommetsVisites.contains(suivant)) {
    			parcoursProfondeur(suivant.getI(),suivant.getJ(), sommetsVisites);
    		}
    	}
    }
    /**
     * Cette fonction permet d'obtenir une liste des pions adjacents à celui se trouvant 
     * en (i,j) et de la même couleur que celui-ci.
     * @param i ligne
     * @param j colonne
     * @return une liste des pions voisins du même joueur
     */
    private ArrayList<Couple> voisin(int i,int j){
    	ArrayList<Couple> res = new ArrayList<Couple>();
    	if(i+1<DIMENSION && i+1>=0 && j+1<DIMENSION && j+1>=0)
    	{
    		if(this.tab[i][j]==this.tab[i+1][j+1]) {
    			Couple c = new Couple(i+1,j+1);
    			res.add(c);
    		}
    	}
    	if(i<DIMENSION && i>=0 && j+1<DIMENSION && j+1>=0)
    	{
    		if(this.tab[i][j]==this.tab[i][j+1]) {
    			Couple c = new Couple(i,j+1);
    			res.add(c);
    		}
    	}
    	if(i-1<DIMENSION && i-1>=0 && j+1<DIMENSION && j+1>=0)
    	{
    		if(this.tab[i][j]==this.tab[i-1][j+1]) {
    			Couple c = new Couple(i-1,j+1);
    			res.add(c);
    		}
    	}
    	if(i+1<DIMENSION && i+1>=0 && j<DIMENSION && j>=0)
    	{
    		if(this.tab[i][j]==this.tab[i+1][j]) {
    			Couple c = new Couple(i+1,j);
    			res.add(c);
    		}
    	}
    	if(i+1<DIMENSION && i+1>=0 && j-1<DIMENSION && j-1>=0)
    	{
    		if(this.tab[i][j]==this.tab[i+1][j-1]) {
    			Couple c = new Couple(i+1,j-1);
    			res.add(c);
    		}
    	}
    	if(i-1<DIMENSION && i-1>=0 && j-1<DIMENSION && j-1>=0)
    	{
    		if(this.tab[i][j]==this.tab[i-1][j-1]) {
    			Couple c = new Couple(i-1,j-1);
    			res.add(c);
    		}
    	}
    	if(i<DIMENSION && i>=0 && j-1<DIMENSION && j-1>=0)
    	{
    		if(this.tab[i][j]==this.tab[i][j-1]) {
    			Couple c = new Couple(i,j-1);
    			res.add(c);
    		}
    	}
    	if(i-1<DIMENSION && i-1>=0 && j<DIMENSION && j>=0)
    	{
    		if(this.tab[i][j]==this.tab[i-1][j]) {
    			Couple c = new Couple(i-1,j);
    			res.add(c);
    		}
    	}
    	return res;
    }
    
    /* (non-Javadoc)
     * @see moteur.Partie1#play(java.lang.String, java.lang.String)
     */
    @Override
    public void play(String move, String player) {
            Couple[] c = trad(move);
            tab[c[1].getI()][c[1].getJ()] = tab[c[0].getI()][c[0].getJ()];
            tab[c[0].getI()][c[0].getJ()] = 0;
            this.updateNbPion();
    }

    @Override
    public void saveToFile(String fileName) {
        // TODO tazzzz : Auto-generated method stub
    	String ligne="";
    	char c;
    	try {
			BufferedWriter wr = new BufferedWriter(new FileWriter(fileName,false));
			for(int i = 0 ;i<DIMENSION; i++) {
				for(int j = 0 ;j<DIMENSION; j++) {
					if(this.tab[i][j] == NOIR)
						c = PION_NOIR;
					else if (this.tab[i][j] == BLANC)
						c = PION_BLANC;
					else 
						c = '.';
					ligne += c+" ";
				}
				wr.write(ligne);
				wr.newLine();
				ligne = "";
			}
			wr.flush();
			wr.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Probleme survenu lors de l'écriture du fichier \""+fileName+"\" .");
		}
    }

    @Override
    public void setFromFile(String fileName) {
        // TODO tazzzz : Auto-generated method stub
    	String str = "";
    	String line = "";
    	int ligne = 0 ,colonne = 0;
    	StringTokenizer tk;
    	byte b;
    	char element;
    	try {
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			while ((line = in.readLine()) != null) {
				colonne = 0;
				str += line+"\n";
				tk = new StringTokenizer(line);
				if(tk.countTokens() < DIMENSION){
					throw new IOException(); 
				} else {
					while(tk.hasMoreTokens()){
						element = tk.nextToken().toCharArray()[0];
						if(element == PION_NOIR) {
							b = NOIR;
						} else if (element == PION_BLANC) {
							b = BLANC;
						} else 
							b = 0;
						this.tab[ligne][colonne]=b;
						colonne++;
					}
					ligne++;
				}
				
			}
			in.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Le fichier spécifier n'a pas été trouvé.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Il y a eu un probleme lors de la lecture du fichier. Le nombre d'élément lu ne correspond pas.");
		}

    }

    /**
     * @return the tab
     */
    public byte[][] getTab() {
        return tab;
    }

    /**
     * @param tab the tab to set
     */
    public void setTab(byte[][] tab) {
        this.tab = tab;
    }

    public String toString() {
    	String ligne="";
    	char c;
    	for(int j = DIMENSION-1 ;j>=0; j--) {	
    		for(int i = 0 ;i<DIMENSION; i++) {
				if(this.tab[i][j] == NOIR)
					c = PION_NOIR;
				else if (this.tab[i][j] == BLANC)
					c = PION_BLANC;
				else 
					c = '.';
				ligne += c+" ";
			}
			ligne+="\n";
    	}
    	return ligne;
    }
    
    
    /**
     * Dresse la liste des coups possible pour un joueur donnée
     * @param cJoueur la couleur permettant d'identifier le joueur
     * @return un objet de type ArrayList<String> contenant l'ensemble de coup possible pour le joueur cJoueur
     * à un instant donné
     */
    public ArrayList<String> coupsPossibles (String cJoueur) {
    	ArrayList<String> resultat = new ArrayList<String>();
    	int i = 0 , j,nbPion,verticale,horizontal,slash,antislash;
    	byte couleurId;
    	if(cJoueur.compareToIgnoreCase("noir") == 0) { 
    		nbPion = this.nbPionNoir;
    		couleurId = this.NOIR;
    	} else {
    		nbPion = this.nbPionBlanc;
    		couleurId = this.BLANC;
    	}
    	while(i<this.DIMENSION && nbPion > 0) {
    		j=0;
    		while(j<this.DIMENSION && nbPion > 0) {
    			if(this.tab[i][j] == couleurId) {
    				verticale=this.compteVerticale(i);
    				horizontal=this.compteHorizonztale(j);
    				slash=this.compteSlash(i, j);
    				antislash=this.compteAntiSlash(i, j);
    				Couple origine = new Couple(i,j);
    				Couple destination = null;
    				Couple [] tabCoup = new Couple [8];
    				if (j+verticale < DIMENSION)
    					tabCoup[0] = new Couple (i,j+verticale);
    				else 
    					tabCoup[0] = null;
    				if (j-verticale < DIMENSION)
    					tabCoup[1] = new Couple (i,j-verticale);
    				else
    					tabCoup[1] = null;
    				if(i+horizontal < DIMENSION)
    					tabCoup[2] = new Couple (i+horizontal,j);
    				else
    					tabCoup[2] = null;
    				if (i-horizontal < DIMENSION) 
    					tabCoup[3] = new Couple (i-horizontal,j);
    				else
    					tabCoup[3] = null;
    				if(i-antislash< DIMENSION && j+antislash < DIMENSION)
    					tabCoup[4] = new Couple (i-antislash,j+antislash);
    				else
    					tabCoup[4] = null;
    				if(i+antislash < DIMENSION && j-antislash < DIMENSION)
    					tabCoup[5] = new Couple (i+antislash,j-antislash);
    				else
    					tabCoup[5] = null;
    				if(i+slash < DIMENSION && j+slash < DIMENSION)
    					tabCoup[6] = new Couple (i+slash,j+slash);
    				else
    					tabCoup[6] = null;
    				if(i-slash < DIMENSION && j-slash < DIMENSION)
    					tabCoup[7] = new Couple (i-slash,j-slash);
    				else
    					tabCoup[7] = null;
    				for(int v = 0;v<=7;v++) {
    					if(tabCoup[v] != null) {
		    				destination = tabCoup[v];
		    				String toto = origine.toString()+"-"+destination.toString();
	    					if(this.estValide(origine.toString()+"-"+destination.toString(), cJoueur)){
		    					resultat.add(origine.toString()+"-"+destination.toString());
		    				}
    					}
    				}
    				nbPion --;
    			}
    			j++;
    		}
    		i++;
    	}
    	return resultat;
    }
    
    /**
     * fonction de mise à jour du nombre de pion présent sur le plateau de jeu
     */
    private void updateNbPion() {
    	int noir = 0 , blanc = 0;
    	for(int i = 0 ;i<DIMENSION; i++)	
    		for(int j = 0 ;j<DIMENSION; j++) {
    			if(this.getTab()[i][j] == BLANC)
    				blanc++;
    			else if (this.getTab()[i][j] == NOIR)
    				noir++;
    		}
    	this.nbPionBlanc = blanc;
    	this.nbPionNoir = noir;
    }
    
    /**
     * realise un copie du plateau de jeu (duplique)
     * @return la copie du plateau de jeu
     */
    public Plateau copy(){
    	Plateau p = new Plateau();
    	for(int i=0;i<this.DIMENSION;i++)
    		for(int j = 0 ; j<this.DIMENSION;j++){
    			p.tab[i][j]=this.tab[i][j];
    		}
    	p.nbPionBlanc=this.nbPionBlanc;
    	p.nbPionNoir=this.nbPionNoir;
    	return p;
    }
    
}