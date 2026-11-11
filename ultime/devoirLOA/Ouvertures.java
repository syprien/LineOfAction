package ultime.devoirLOA;

import java.util.HashMap;

import ultime.moteur.Plateau;

/**
 * ULTIME (2026) : bibliothèque d'ouvertures, calculée à l'avance par tests/GenererOuvertures
 * (une recherche de 10 secondes par position). Elle remplace les coups aléatoires de 2008 :
 * les deux premiers coups de la partie sont joués instantanément, et sont mieux réfléchis
 * qu'en partie. Une position est reconnue par son empreinte de Zobrist et le joueur au trait.
 *
 * @author Syprien
 */
public final class Ouvertures {
    private static final HashMap<String, String> TABLE = new HashMap<>();

    private static void ajouter(long hash, String joueur, String coup) {
        TABLE.put(joueur + Long.toHexString(hash), coup);
    }

    /** Le coup de la bibliothèque pour cette position, ou null si elle n'y figure pas. */
    public static String coup(Plateau p, String joueur) {
        return TABLE.get(joueur + Long.toHexString(p.getHash()));
    }

    static {
        ajouter(0xebaa9f0f923710e1L, "noir", "B1-B3");
        ajouter(0x8a4ab6559cf2b14L, "blanc", "H3-E3");
        ajouter(0x24f0058a4cd4610aL, "blanc", "A2-C4");
        ajouter(0xd1f569270d539f8aL, "blanc", "H3-E3");
        ajouter(0x88f974b93cc265f6L, "blanc", "H6-E6");
        ajouter(0xf154549a469560fdL, "blanc", "A7-C5");
        ajouter(0x722983f3238be94L, "blanc", "H6-E6");
        ajouter(0xe567bf46f20ef29fL, "blanc", "H3-E3");
        ajouter(0xa0cb6cdca90467d1L, "blanc", "H3-E3");
        ajouter(0xa70185314630438bL, "blanc", "A7-C7");
        ajouter(0x43cc762c411a8835L, "blanc", "H6-E6");
        ajouter(0xbcd9a3740b40e86cL, "blanc", "A2-C2");
        ajouter(0xd579f838bfc8ce8L, "blanc", "H6-E6");
        ajouter(0x99422455ad15c64dL, "blanc", "H3-E3");
        ajouter(0x350e76102ef24c81L, "blanc", "A2-C2");
        ajouter(0x4013e617f98972d3L, "blanc", "H3-E3");
        ajouter(0x916cefee9053e5c3L, "blanc", "H6-E6");
        ajouter(0x1eb703689ea93ea1L, "blanc", "H6-E6");
        ajouter(0xc37c4a377e6957d1L, "blanc", "A6-D6");
        ajouter(0x7a38904b6bf83f4bL, "blanc", "H3-E3");
        ajouter(0xab7fe4deb2e72174L, "blanc", "A3-D3");
        ajouter(0x3f9443d130f2aa05L, "blanc", "H2-F2");
        ajouter(0x9b8f12a871a7583dL, "blanc", "A6-D6");
        ajouter(0xd514fb07bb415ce0L, "blanc", "H7-F7");
        ajouter(0x42b66ca9b5c5893fL, "blanc", "A6-D6");
        ajouter(0x417e1a0fc71cf865L, "blanc", "A3-D3");
        ajouter(0xb79931c28ad98b64L, "blanc", "H7-F7");
        ajouter(0xed32484a44fb72a9L, "blanc", "A3-D3");
        ajouter(0x66da2acdbbb5de15L, "blanc", "A6-D6");
        ajouter(0x34ca8f14558f6c07L, "blanc", "A6-D6");
        ajouter(0xae06ab026c830d04L, "blanc", "H2-F2");
        ajouter(0x6068788a48492f1dL, "blanc", "H3-F5");
        ajouter(0x6294d2e8a533e68dL, "blanc", "H2-F4");
        ajouter(0xb12f0c1f91563122L, "blanc", "A3-D3");
        ajouter(0xe243b4b8aa32ae98L, "blanc", "A6-D6");
        ajouter(0x72b016d9380bcab8L, "blanc", "H7-F5");
        ajouter(0x3b7acab96e507f9aL, "blanc", "A6-D6");
    }

    private Ouvertures() { }
}
