// Réécrit en 2026 d'après le protocole de 2008 ; l'original était fourni par l'enseignant.
package net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Client réseau du tournoi : se connecte à l'arbitre et fait le lien avec un {@link JoueurReseau}.
 *
 * Protocole (messages de l'arbitre terminés par '\0', sauf « A VOUS DE JOUER ») :
 * <pre>
 *   client  : identifiant
 *   arbitre : NOMBRE DE JOUEURS : 2
 *             INDENTIFIANTS JOUEURS : noir blanc      (sic)
 *             VOTRE IDENTIFIANT : noir
 *             COMMENCE noir
 *   boucle  : MOUVEMENT noir : B1-D3
 *             MOUVEMENT_ILLEGAL noir : motif
 *             A VOUS DE JOUER ! ... :temps_restant_ms   -> le client répond « coup\0 »
 *   fin     : deux lignes (résultat)
 * </pre>
 */
public class ClientJeu {

    private final JoueurReseau joueur;
    private final String identifiant;

    public ClientJeu(String machine, int port, JoueurReseau j) {
        this(machine, port, j, "tazzzz_syprien");
    }

    public ClientJeu(String machine, int port, JoueurReseau j, String idJoueur) {
        this.joueur = j;
        this.identifiant = idJoueur;
        try (Socket socket = new Socket(machine, port)) {
            System.out.println("Connexion à " + machine + ":" + port);
            dialoguer(new BufferedReader(new InputStreamReader(socket.getInputStream())),
                      new PrintWriter(socket.getOutputStream(), true));
        } catch (IOException e) {
            System.out.println("Connexion à " + machine + ":" + port + " impossible ou interrompue : " + e.getMessage());
        }
    }

    /** Retire le '\0' final et l'en-tête d'un message de l'arbitre. */
    private static String contenu(String ligne, String entete) {
        String s = ligne.substring(entete.length());
        return s.endsWith("\0") ? s.substring(0, s.length() - 1) : s;
    }

    private static String attendre(BufferedReader in, String entete) throws IOException {
        String ligne = in.readLine();
        if (ligne == null || !ligne.startsWith(entete))
            throw new IOException("protocole : « " + entete + " » attendu, reçu « " + ligne + " »");
        return contenu(ligne, entete).trim();
    }

    private void dialoguer(BufferedReader in, PrintWriter out) throws IOException {
        out.println(identifiant);
        System.out.println("[Arbitre] nombre de joueurs : " + attendre(in, "NOMBRE DE JOUEURS : "));
        joueur.setJoueurIds(attendre(in, "INDENTIFIANTS JOUEURS : ").split(" "));
        joueur.setId(attendre(in, "VOTRE IDENTIFIANT : "));
        joueur.premierJoueur(attendre(in, "COMMENCE "));

        while (!joueur.partieTerminee()) {
            String ligne = in.readLine();
            if (ligne == null) {
                System.out.println("[Arbitre] connexion fermée");
                return;
            }
            System.out.println("[Arbitre] " + ligne.replace("\0", ""));
            if (ligne.startsWith("MOUVEMENT_ILLEGAL ")) {
                String[] m = contenu(ligne, "MOUVEMENT_ILLEGAL ").split(" : ", 2);
                joueur.disqualifier(m[0], m.length > 1 ? m[1] : "");
            } else if (ligne.startsWith("MOUVEMENT ")) {
                String[] m = contenu(ligne, "MOUVEMENT ").split(" : ", 2);
                joueur.jouer(m[0], m[1]);
            } else if (ligne.startsWith("A VOUS DE JOUER")) {
                String temps = ligne.substring(ligne.lastIndexOf(':') + 1).replace("\0", "").trim();
                joueur.setTemp(Integer.parseInt(temps));
                String coup = joueur.choisirCoup();
                System.out.println("Je joue : " + coup);
                out.println(coup + '\0');
            }
        }
        for (int i = 0; i < 2; i++) {
            String ligne = in.readLine();
            if (ligne == null) return;
            System.out.println(ligne.replace("\0", ""));
        }
    }
}
