package ultime.devoirLOA;

import net.JoueurReseau;
import ultime.jeux.alg.NegaScout;
import ultime.moteur.Plateau;

/**
 * ULTIME (2026) : le joueur réseau. Plus de coups au hasard ni de « mode panique » :
 * l'approfondissement itératif s'arrête de lui-même quand le temps accordé au coup est écoulé.
 * Chaque coup reçoit 1/25 du temps restant (une partie dure environ 25 coups, comme
 * l'estimait le rapport de 2008).
 *
 * @author Syprien
 */
public class JoueurReseauIALOA implements JoueurReseau {
    private final Plateau p = new Plateau();
    private final NegaScout recherche = new NegaScout();
    private String id = "noir";
    private int temps = 300000;
    private boolean disqualification;

    public String choisirCoup() {
        long debut = System.nanoTime(), noeuds = recherche.noeuds();
        String livre = Ouvertures.coup(p, id);
        if (livre != null && p.coupsPossibles(id).contains(livre)) {
            System.out.println("coup " + livre + " livre oui");
            return livre;
        }
        long budget = Math.max(20, Math.min(temps / 25, temps - 500));
        String coup = recherche.meilleurCoup(p, id, budget);
        // ULTIME (2026) : une ligne de trace par coup, au même format que la version Rust (ultime-rs).
        System.out.println("coup " + coup + " profondeur " + recherche.profondeurAtteinte() + " noeuds "
                + (recherche.noeuds() - noeuds) + " score " + recherche.score() + " temps "
                + (System.nanoTime() - debut) / 1000000 + " ms livre non");
        return coup != null ? coup : "A1-A1";
    }

    public void jouer(String joueurId, String coup) {
        p.play(coup, joueurId.toLowerCase());
    }

    public void disqualifier(String joueurId, String motif) {
        disqualification = true;
    }

    public boolean partieTerminee() {
        return disqualification || p.finDePartie();
    }

    public void setId(String joueurId) {
        id = joueurId.toLowerCase();
    }

    public void setTemp(int tps) {
        temps = tps;
    }

    public void premierJoueur(String joueurId) { }

    public void setJoueurIds(String[] lesJoueurIds) { }
}
