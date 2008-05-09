// Réécrit en 2026 d'après le protocole de 2008 ; l'original était fourni par l'enseignant.
package net;

/**
 * Ce qu'un joueur doit savoir faire pour participer au tournoi par le réseau.
 * C'est {@link ClientJeu} qui appelle ces méthodes au fil des messages de l'arbitre.
 */
public interface JoueurReseau {

    /** Identifiants de tous les joueurs de la partie (par exemple « noir » et « blanc »). */
    void setJoueurIds(String[] lesJoueurIds);

    /** Identifiant attribué à ce joueur par l'arbitre. */
    void setId(String joueurId);

    /** Identifiant du joueur qui commence. */
    void premierJoueur(String joueurId);

    /** Renvoie le coup à jouer, sous la forme « B1-D3 ». C'est ici que réside l'IA. */
    String choisirCoup();

    /** Un coup a été joué (y compris par ce joueur) : mettre à jour son plateau. */
    void jouer(String joueurId, String coup);

    /** Un joueur est disqualifié, pour le motif indiqué. */
    void disqualifier(String joueurId, String motif);

    /** Vrai quand la partie est terminée. */
    boolean partieTerminee();

    /** Temps de réflexion restant à ce joueur, en millisecondes, envoyé avant chaque coup. */
    void setTemp(int tps);
}
