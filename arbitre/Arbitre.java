package arbitre;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Arbitre du tournoi de Lines of Action, reconstitué en 2026 d'après le protocole
 * attendu par net/ClientJeu.java (le serveur d'origine n'a pas été archivé).
 *
 * Deux modes :
 *   - serveur seul : attend 2 clients sur le port ; le premier connecté joue noir.
 *         java -jar Arbitre.jar --port 1234
 *         java -jar TazzzzSyprien.jar localhost 1234 TazzzzSyprien   (x2, dans d'autres terminaux)
 *   - match        : lance lui-même les deux jars et inverse les couleurs à chaque partie.
 *         java -jar Arbitre.jar --j1 TazzzzSyprien.jar --j2 TazzzzSyprien_v2008-rendu.jar --parties 4
 *
 * Les règles sont codées ici indépendamment de moteur.Plateau, exprès : un arbitre qui
 * partagerait le code de plateau des joueurs partagerait aussi ses bugs.
 *
 * Protocole (les lignes serveur -> client finissent par '\0', sauf "A VOUS DE JOUER" dont le
 * dernier morceau après ':' est lu comme un entier par le client) :
 *   client: <identifiant>
 *   serveur: NOMBRE DE JOUEURS : 2\0
 *           INDENTIFIANTS JOUEURS : noir blanc\0     (sic, "INDENTIFIANTS")
 *           VOTRE IDENTIFIANT : noir|blanc\0
 *           COMMENCE noir\0
 *   boucle: A VOUS DE JOUER ! TEMPS RESTANT (ms) :<ms>   -> le client répond "B1-B3\0"
 *           MOUVEMENT <couleur> : <coup>\0              (envoyé aux deux joueurs)
 *           MOUVEMENT_ILLEGAL <couleur> : <motif>\0     (envoyé aux deux joueurs)
 *   fin:    deux lignes : FIN DE PARTIE\0 puis GAGNANT : ...\0 / MATCH NUL : ...\0
 */
public class Arbitre {

    static final int N = 8, VIDE = 0, NOIR = 1, BLANC = -1;
    static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
    static final Pattern COUP = Pattern.compile("([A-H])([1-8])-([A-H])([1-8])");

    int port = 1234;
    long temps = 300000;
    int parties = 1;
    int maxCoups = 300;
    String jar1, jar2;
    File logs;
    boolean plateau = false;

    public static void main(String[] args) throws Exception {
        Arbitre a = new Arbitre();
        try {
            a.lireOptions(args);
        } catch (RuntimeException e) {
            System.err.println("Erreur : " + e.getMessage());
            usage();
            System.exit(1);
        }
        a.lancer();
    }

    static void usage() {
        System.err.println(String.join("\n",
                "Usage : java -jar Arbitre.jar [options]",
                "  --port <n>        port d'ecoute (defaut 1234)",
                "  --temps <ms>      temps total par joueur et par partie (defaut 300000 = 5 min)",
                "  --parties <n>     nombre de parties (defaut 1)",
                "  --max-coups <n>   partie nulle apres n coups (defaut 300)",
                "  --j1 <jar> --j2 <jar>  lance les deux joueurs automatiquement",
                "                         (couleurs alternees a chaque partie)",
                "  --logs <dossier>  sortie console des joueurs (sinon ignoree)",
                "  --plateau         affiche le plateau apres chaque coup"));
    }

    void lireOptions(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String o = args[i];
            if (o.equals("--plateau")) { plateau = true; continue; }
            if (o.equals("-h") || o.equals("--help")) { usage(); System.exit(0); }
            if (i + 1 >= args.length) throw new IllegalArgumentException("valeur manquante pour " + o);
            String v = args[++i];
            switch (o) {
                case "--port": port = Integer.parseInt(v); break;
                case "--temps": temps = Long.parseLong(v); break;
                case "--parties": parties = Integer.parseInt(v); break;
                case "--max-coups": maxCoups = Integer.parseInt(v); break;
                case "--j1": jar1 = v; break;
                case "--j2": jar2 = v; break;
                case "--logs": logs = new File(v); break;
                default: throw new IllegalArgumentException("option inconnue " + o);
            }
        }
        if ((jar1 == null) != (jar2 == null)) throw new IllegalArgumentException("--j1 et --j2 vont ensemble");
        for (String j : new String[]{jar1, jar2})
            if (j != null && !new File(j).isFile()) throw new IllegalArgumentException("fichier introuvable : " + j);
    }

    // =====================================================================================
    // Règles de Lines of Action. Plateau t[colonne][ligne], A1 = t[0][0], comme moteur.Plateau.
    // =====================================================================================

    static int[][] plateauInitial() {
        int[][] t = new int[N][N];
        for (int k = 1; k < N - 1; k++) {
            t[k][0] = NOIR;  t[k][N - 1] = NOIR;   // lignes 1 et 8
            t[0][k] = BLANC; t[N - 1][k] = BLANC;  // colonnes A et H
        }
        return t;
    }

    static boolean dansPlateau(int c, int l) {
        return c >= 0 && c < N && l >= 0 && l < N;
    }

    /** Nombre de pions (des deux couleurs) sur toute la ligne passant par (c,l) dans la direction (dc,dl). */
    static int piecesSurLaLigne(int[][] t, int c, int l, int dc, int dl) {
        int n = 0;
        for (int x = c, y = l; dansPlateau(x, y); x += dc, y += dl) if (t[x][y] != VIDE) n++;
        for (int x = c - dc, y = l - dl; dansPlateau(x, y); x -= dc, y -= dl) if (t[x][y] != VIDE) n++;
        return n;
    }

    static int[] lireCoup(String coup) {
        Matcher m = COUP.matcher(coup);
        if (!m.matches()) return null;
        return new int[]{m.group(1).charAt(0) - 'A', m.group(2).charAt(0) - '1',
                         m.group(3).charAt(0) - 'A', m.group(4).charAt(0) - '1'};
    }

    static String nomCase(int c, int l) {
        return "" + (char) ('A' + c) + (l + 1);
    }

    /** @return null si le coup est légal, sinon la raison pour laquelle il ne l'est pas. */
    static String verifier(int[][] t, int couleur, int[] m) {
        int c0 = m[0], l0 = m[1], c1 = m[2], l1 = m[3];
        if (t[c0][l0] != couleur) return "pas de pion du joueur en " + nomCase(c0, l0);
        int dc = c1 - c0, dl = l1 - l0;
        if (dc == 0 && dl == 0) return "deplacement nul";
        if (dc != 0 && dl != 0 && Math.abs(dc) != Math.abs(dl)) return "ni ligne ni diagonale";
        int sc = Integer.signum(dc), sl = Integer.signum(dl);
        int distance = Math.max(Math.abs(dc), Math.abs(dl));
        int attendu = piecesSurLaLigne(t, c0, l0, sc, sl);
        if (distance != attendu) return "distance " + distance + " au lieu de " + attendu;
        for (int k = 1; k < distance; k++)
            if (t[c0 + k * sc][l0 + k * sl] == -couleur) return "saute un pion adverse en " + nomCase(c0 + k * sc, l0 + k * sl);
        if (t[c1][l1] == couleur) return "arrivee sur son propre pion en " + nomCase(c1, l1);
        return null;
    }

    static List<String> coupsLegaux(int[][] t, int couleur) {
        List<String> res = new ArrayList<>();
        for (int c = 0; c < N; c++)
            for (int l = 0; l < N; l++) {
                if (t[c][l] != couleur) continue;
                for (int[] d : DIRECTIONS) {
                    int n = piecesSurLaLigne(t, c, l, d[0], d[1]);
                    int c1 = c + d[0] * n, l1 = l + d[1] * n;
                    if (dansPlateau(c1, l1) && verifier(t, couleur, new int[]{c, l, c1, l1}) == null)
                        res.add(nomCase(c, l) + "-" + nomCase(c1, l1));
                }
            }
        return res;
    }

    /** Vrai si tous les pions de cette couleur forment un seul groupe (8-connexité). */
    static boolean connecte(int[][] t, int couleur) {
        int total = 0, sc = -1, sl = -1;
        for (int c = 0; c < N; c++)
            for (int l = 0; l < N; l++)
                if (t[c][l] == couleur) { total++; sc = c; sl = l; }
        if (total == 0) return false;
        boolean[][] vu = new boolean[N][N];
        int[] pile = new int[N * N];
        int sommet = 0, trouves = 0;
        pile[sommet++] = sc * N + sl;
        vu[sc][sl] = true;
        while (sommet > 0) {
            int p = pile[--sommet];
            trouves++;
            for (int dc = -1; dc <= 1; dc++)
                for (int dl = -1; dl <= 1; dl++) {
                    int x = p / N + dc, y = p % N + dl;
                    if (dansPlateau(x, y) && !vu[x][y] && t[x][y] == couleur) {
                        vu[x][y] = true;
                        pile[sommet++] = x * N + y;
                    }
                }
        }
        return trouves == total;
    }

    static String afficher(int[][] t) {
        StringBuilder sb = new StringBuilder("      A B C D E F G H\n");
        for (int l = N - 1; l >= 0; l--) {
            sb.append("    ").append(l + 1).append(' ');
            for (int c = 0; c < N; c++)
                sb.append(t[c][l] == NOIR ? 'x' : t[c][l] == BLANC ? 'o' : '.').append(' ');
            sb.append(l + 1).append('\n');
        }
        return sb.append("      A B C D E F G H").toString();
    }

    // =====================================================================================
    // Réseau
    // =====================================================================================

    static class Joueur {
        final String id;
        final Socket socket;
        final BufferedReader in;
        final PrintWriter out;
        int couleur;
        String nomCouleur;
        long temps;

        Joueur(Socket s, String id, BufferedReader in) throws IOException {
            this.socket = s;
            this.id = id;
            this.in = in;
            this.out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.ISO_8859_1));
        }

        void envoyer(String message) {
            out.print(message + "\n");
            out.flush();
        }

        void fermer() {
            try { socket.close(); } catch (IOException ignored) { }
        }
    }

    static class Issue {
        final Joueur gagnant;
        final String raison;
        final int coups;
        Issue(Joueur gagnant, String raison, int coups) { this.gagnant = gagnant; this.raison = raison; this.coups = coups; }
    }

    static String nettoyer(String s) {
        return s == null ? "" : s.replace("\0", "").trim();
    }

    static Joueur accepter(ServerSocket ss) throws IOException {
        Socket s = ss.accept();
        s.setSoTimeout(10000);
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.ISO_8859_1));
        String id = nettoyer(in.readLine());
        System.out.println("  connexion de \"" + id + "\" (" + s.getRemoteSocketAddress() + ")");
        return new Joueur(s, id, in);
    }

    Issue jouer(Joueur noir, Joueur blanc) {
        noir.couleur = NOIR;   noir.nomCouleur = "noir";
        blanc.couleur = BLANC; blanc.nomCouleur = "blanc";
        Joueur[] js = {noir, blanc};
        for (Joueur j : js) {
            j.temps = temps;
            j.envoyer("NOMBRE DE JOUEURS : 2\0");
            j.envoyer("INDENTIFIANTS JOUEURS : noir blanc\0");
            j.envoyer("VOTRE IDENTIFIANT : " + j.nomCouleur + "\0");
        }
        for (Joueur j : js) j.envoyer("COMMENCE noir\0");

        int[][] t = plateauInitial();
        int tour = 0, coups = 0, passes = 0;
        Issue issue = null;
        while (issue == null) {
            Joueur j = js[tour], adv = js[1 - tour];
            if (coups >= maxCoups) {
                issue = new Issue(null, "limite de " + maxCoups + " coups atteinte", coups);
                break;
            }
            if (coupsLegaux(t, j.couleur).isEmpty()) {
                // Règle de LOA : un joueur sans coup légal passe son tour.
                System.out.println("       " + j.nomCouleur + " ne peut pas jouer et passe");
                if (++passes == 2) issue = new Issue(null, "aucun joueur ne peut jouer", coups);
                tour = 1 - tour;
                continue;
            }
            passes = 0;

            j.envoyer("A VOUS DE JOUER ! TEMPS RESTANT (ms) :" + j.temps);
            long debut = System.nanoTime();
            String reponse = null, motif = null;
            try {
                j.socket.setSoTimeout((int) Math.min(Integer.MAX_VALUE, j.temps + 1000));
                reponse = j.in.readLine();
                if (reponse == null) motif = "deconnexion";
            } catch (SocketTimeoutException e) {
                motif = "temps depasse";
            } catch (IOException e) {
                motif = "deconnexion";
            }
            long ecoule = (System.nanoTime() - debut) / 1000000;
            j.temps -= ecoule;

            String coup = nettoyer(reponse).toUpperCase(Locale.ROOT);
            int[] m = null;
            if (motif == null && j.temps < 0) motif = "temps depasse";
            if (motif == null) {
                m = lireCoup(coup);
                motif = (m == null) ? "coup mal forme '" + coup + "'" : verifier(t, j.couleur, m);
            }
            if (motif != null) {
                for (Joueur x : js) x.envoyer("MOUVEMENT_ILLEGAL " + j.nomCouleur + " : " + motif + "\0");
                issue = new Issue(adv, j.nomCouleur + " disqualifie (" + motif + (coup.isEmpty() ? "" : ", coup " + coup) + ")", coups);
                break;
            }

            t[m[2]][m[3]] = t[m[0]][m[1]];
            t[m[0]][m[1]] = VIDE;
            coups++;
            for (Joueur x : js) x.envoyer("MOUVEMENT " + j.nomCouleur + " : " + coup + "\0");
            System.out.printf(Locale.ROOT, "  %3d. %-5s %s   (%5.2f s, reste %5.1f s)%n",
                    coups, j.nomCouleur, coup, ecoule / 1000.0, j.temps / 1000.0);
            if (plateau) System.out.println(afficher(t));
            System.out.flush();

            boolean n = connecte(t, NOIR), b = connecte(t, BLANC);
            if (n || b) {
                // Si un coup relie les deux camps à la fois, le joueur qui vient de jouer gagne.
                Joueur g = (n && b) ? j : (n ? noir : blanc);
                issue = new Issue(g, g.nomCouleur + " a relie tous ses pions", coups);
            }
            tour = 1 - tour;
        }

        for (Joueur x : js) {
            x.envoyer("FIN DE PARTIE\0");
            x.envoyer(issue.gagnant == null ? "MATCH NUL : " + issue.raison + "\0"
                    : "GAGNANT : " + issue.gagnant.nomCouleur + " (" + issue.gagnant.id + ")\0");
            x.fermer();
        }
        if (!plateau) System.out.println(afficher(t));
        return issue;
    }

    static String nomJar(String jar) {
        String n = new File(jar).getName();
        return n.toLowerCase(Locale.ROOT).endsWith(".jar") ? n.substring(0, n.length() - 4) : n;
    }

    Process lancerJar(String jar, String id, int partie) throws IOException {
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        ProcessBuilder pb = new ProcessBuilder(java, "-jar", jar, "localhost", String.valueOf(port), id);
        pb.redirectErrorStream(true);
        if (logs != null) {
            logs.mkdirs();
            pb.redirectOutput(new File(logs, "partie" + partie + "_" + id + ".log"));
        } else {
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        }
        return pb.start();
    }

    void lancer() throws Exception {
        boolean auto = jar1 != null;
        String id1 = auto ? nomJar(jar1) : null, id2 = auto ? nomJar(jar2) : null;
        if (auto && id1.equals(id2)) { id1 += "_1"; id2 += "_2"; }
        int[] victoires = new int[2];
        int nuls = 0;
        List<String> bilan = new ArrayList<>();

        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Arbitre LOA sur le port " + port + ", " + temps / 1000 + " s par joueur, "
                    + parties + " partie(s).");
            for (int p = 1; p <= parties; p++) {
                System.out.println("\n=== Partie " + p + " ===");
                Joueur noir, blanc;
                Process[] procs = new Process[0];
                if (auto) {
                    boolean j1Noir = (p % 2 == 1);
                    String idNoir = j1Noir ? id1 : id2, idBlanc = j1Noir ? id2 : id1;
                    procs = new Process[]{lancerJar(j1Noir ? jar1 : jar2, idNoir, p), lancerJar(j1Noir ? jar2 : jar1, idBlanc, p)};
                    ss.setSoTimeout(30000);
                    Joueur a = accepter(ss), b = accepter(ss);
                    noir = a.id.equals(idNoir) ? a : b;
                    blanc = (noir == a) ? b : a;
                    if (!noir.id.equals(idNoir) || !blanc.id.equals(idBlanc))
                        throw new IllegalStateException("identifiants inattendus : " + a.id + ", " + b.id);
                } else {
                    System.out.println("En attente de 2 joueurs (le premier connecte joue noir)...");
                    noir = accepter(ss);
                    blanc = accepter(ss);
                }
                System.out.println("  noir (x) : " + noir.id + "   blanc (o) : " + blanc.id);

                Issue issue = jouer(noir, blanc);
                String res = (issue.gagnant == null ? "match nul" : "gagnant " + issue.gagnant.id)
                        + " : " + issue.raison + ", " + issue.coups + " coups";
                System.out.println("  -> " + res);
                bilan.add("Partie " + p + " (noir " + noir.id + " / blanc " + blanc.id + ") : " + res);
                if (issue.gagnant == null) nuls++;
                else victoires[auto ? (issue.gagnant.id.equals(id1) ? 0 : 1) : (issue.gagnant == noir ? 0 : 1)]++;

                for (Process pr : procs)
                    if (!pr.waitFor(5, TimeUnit.SECONDS)) pr.destroyForcibly();
            }
        }

        System.out.println("\n=== Bilan ===");
        for (String l : bilan) System.out.println(l);
        if (auto) System.out.println(id1 + " " + victoires[0] + " - " + victoires[1] + " " + id2 + "   (nuls : " + nuls + ")");
        else System.out.println("noir " + victoires[0] + " - " + victoires[1] + " blanc   (nuls : " + nuls + ")");
    }
}
