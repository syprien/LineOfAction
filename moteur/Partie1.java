// Réécrit en 2026 d'après le protocole de 2008 ; l'original était fourni par l'enseignant.
package moteur;

/**
 * Ce que le plateau de Lines of Action doit savoir faire (première partie du projet).
 *
 * Format des fichiers de plateau : 8 lignes, de la ligne 8 à la ligne 1, chaque case notée
 * « x » (noir), « o » (blanc) ou « . » (vide), séparées par des espaces ; une ligne qui commence
 * par « % » est un commentaire.
 */
public interface Partie1 {

    /** Charge un plateau depuis un fichier texte au format ci-dessus. */
    void setFromFile(String fileName);

    /** Sauvegarde le plateau dans un fichier, au même format. */
    void saveToFile(String fileName);

    /** Vrai si le coup (par exemple « B4-C3 ») est valide pour ce joueur (« noir » ou « blanc »). */
    boolean estValide(String move, String player);

    /** Joue le coup sur le plateau. */
    void play(String move, String player);

    /** Vrai si la partie est terminée. */
    boolean finDePartie();
}
