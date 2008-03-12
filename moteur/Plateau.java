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

public class Plateau implements Partie1 {
	
    static final private byte NOIR = 1;
    static final private byte BLANC = -1;
    static final private char PION_NOIR = 'X';
    static final private char PION_BLANC = 'O';
    static final private byte DIMENSION = 8;
   
    private byte[][] tab;
    private int nbPionNoir;
    private int nbPionBlanc;
    /**
     * @param tab
     */
    public Plateau() {
        this.tab = new byte[DIMENSION][DIMENSION];
        for(int i = 0 ;i<DIMENSION; i++) {
			for(int j = 0 ;j<DIMENSION; j++) {
				this.tab[i][j]=0;
			}
        }
        for(int i=1;i<7;i++) {
        	this.tab[0][i] = NOIR;
        	this.tab[7][i] = NOIR;
        	this.tab[i][0] = BLANC;
        	this.tab[i][7] = BLANC;
        }
        this.nbPionBlanc=12;
        this.nbPionNoir=12;
    }

    @Override
    public boolean estValide(String move, String player) {
        // TODO Syprien : Auto-generated method stub
        return false;
    }

    @Override
    public boolean finDePartie() {
    	int i,j,ok;
    	boolean finNoir=false,
    			finBlanc=false;
    	i=j=ok=0;
    	while (ok<2 && (i<DIMENSION && j <DIMENSION) && !finNoir && !finBlanc){
    		if(this.tab[i][j] == NOIR){
    			finNoir = parcoursChemin(i,j);
    			ok++;
    		}
    		if(this.tab[i][j] == BLANC){
    			finBlanc = parcoursChemin(i,j);
    			ok++;
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
    private boolean parcoursChemin(int i, int j) {
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
     * en (i,j) et de la même couleur que celui-ci
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
    
	@Override
    public void play(String move, String player) {
        // TODO Syprien : Auto-generated method stub

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
			System.out.println("Le fichier spécifier n'a pas été trouvé.")
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
			ligne+="\n";
    	}
    	return ligne;
    }
    public static void main(String args[]) {
    	Plateau p = new Plateau();
    	/* test de la sauvegarde d'un plateau , pour vérifier le résultat il faut 
    	 * ouvrir le fichier test.txt à la racine du workspace.
    	 */
    	p.saveToFile("test.txt");
    	
    	/* test du chargement d'un plateau.
    	 * Pour vérifier facilement le chargement il est préférable de modifier le fichier test.txt "à la main".
    	 */
    	p.setFromFile("test.txt");
    	
    	/* test de la fonction de fin de partie. Afin de faciliter le test il est recommander de modifier le fichier
    	 * directement pour obtenir une fin de partie.
    	 */
    	System.out.println(p.finDePartie());
    	
    	System.out.println(p);
    	/* cas normal un coup valide */
    	System.out.println("Le coup doit être valide : "+p.estValide("B1-D3", "blanc"));
    	/* erreur sur la couleur du pion sur la case d'origine du déplacement */
    	System.out.println("Le coup doit être invalide car le pion n'est pas de la bonne couleur : "+
    			p.estValide("A2-C4", "blanc"));
    	/* nombre de cases trop important */
    	System.out.println("Le coup doit être invalide car le déplacement n'est pas possible : "+
    			p.estValide("A2-D5", "noir"));
    	/* position non atteingnable par le pion */
    	System.out.println("Le coup doit être invalide car la position ne peut être atteinte par le pion : "+
    			p.estValide("A2-B4", "blanc"));
    	/* test de la fonction de deplacement d'un pion */
    	System.out.println(p);
    	p.play("B1-D3","blanc");
    	System.out.println(p);
    	
    }
}