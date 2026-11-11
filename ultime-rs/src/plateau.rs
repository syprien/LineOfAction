//! Le plateau rapide d'Ultime (ultime/moteur/Plateau.java), porté en Rust.
//! Une case est numérotée colonne * 8 + ligne : A1 = 0, A2 = 1, ..., B1 = 8, ..., H8 = 63.
//! Un coup est un entier : départ * 64 + arrivée.

pub const NOIR: i8 = 1;
pub const BLANC: i8 = -1;

/// Le générateur java.util.Random, reproduit à l'identique : mêmes nombres de Zobrist que Java, donc
/// mêmes empreintes, et la bibliothèque d'ouvertures de la version Java reste valable.
pub struct JavaRandom(u64);

impl JavaRandom {
    const MULT: u64 = 0x5DEECE66D;
    const MASQUE: u64 = (1 << 48) - 1;

    pub const fn new(graine: i64) -> Self {
        JavaRandom((graine as u64 ^ Self::MULT) & Self::MASQUE)
    }

    const fn next(&mut self, bits: u32) -> i32 {
        self.0 = (self.0.wrapping_mul(Self::MULT).wrapping_add(0xB)) & Self::MASQUE;
        (self.0 >> (48 - bits)) as u32 as i32
    }

    pub const fn next_long(&mut self) -> u64 {
        let haut = self.next(32) as i64;
        let bas = self.next(32) as i64;
        (haut << 32).wrapping_add(bas) as u64
    }

    pub fn next_int(&mut self, borne: i32) -> i32 {
        if borne & -borne == borne {
            return ((borne as i64 * self.next(31) as i64) >> 31) as i32;
        }
        loop {
            let bits = self.next(31);
            let val = bits % borne;
            if bits.wrapping_sub(val).wrapping_add(borne - 1) >= 0 {
                return val;
            }
        }
    }
}

const fn dedans(c: i32, l: i32) -> bool {
    c >= 0 && c < 8 && l >= 0 && l < 8
}

const DC: [i32; 4] = [0, 1, 1, 1];
const DL: [i32; 4] = [1, 0, 1, -1];

const fn zobrist() -> [[u64; 64]; 2] {
    let mut r = JavaRandom::new(2008);
    let mut t = [[0u64; 64]; 2];
    let mut c = 0;
    while c < 2 {
        let mut s = 0;
        while s < 64 {
            t[c][s] = r.next_long();
            s += 1;
        }
        c += 1;
    }
    t
}

const fn lignes() -> [[u64; 64]; 4] {
    let mut t = [[0u64; 64]; 4];
    let mut s = 0;
    while s < 64 {
        let (c, l) = ((s >> 3) as i32, (s & 7) as i32);
        let mut d = 0;
        while d < 4 {
            let mut k = -7;
            while k <= 7 {
                if dedans(c + k * DC[d], l + k * DL[d]) {
                    t[d][s] |= 1u64 << (s as i32 + k * (8 * DC[d] + DL[d]));
                }
                k += 1;
            }
            d += 1;
        }
        s += 1;
    }
    t
}

const fn voisins() -> [u64; 64] {
    let mut t = [0u64; 64];
    let mut s = 0;
    while s < 64 {
        let (c, l) = ((s >> 3) as i32, (s & 7) as i32);
        let mut dc = -1;
        while dc <= 1 {
            let mut dl = -1;
            while dl <= 1 {
                if (dc != 0 || dl != 0) && dedans(c + dc, l + dl) {
                    t[s] |= 1u64 << (s as i32 + 8 * dc + dl);
                }
                dl += 1;
            }
            dc += 1;
        }
        s += 1;
    }
    t
}

// Toutes les tables sont calculées à la compilation (const fn) : rien à faire au démarrage.
static ZOBRIST: [[u64; 64]; 2] = zobrist();
static LIGNE: [[u64; 64]; 4] = lignes();
/// VOISINS[case] : les 8 cases autour de la case.
pub static VOISINS: [u64; 64] = voisins();

fn indice(couleur: i8) -> usize {
    if couleur == NOIR { 0 } else { 1 }
}

#[derive(Clone)]
pub struct Plateau {
    cases: [i8; 64],
    noirs: u64,
    blancs: u64,
    hash: u64,
    pile_coups: Vec<i32>,
    pile_pris: Vec<i8>,
}

impl Plateau {
    pub fn new() -> Self {
        let mut p = Plateau::vide();
        for i in 1..7 {
            p.poser(i, BLANC); // colonne A, lignes 2 à 7
            p.poser(56 + i, BLANC); // colonne H
            p.poser(i * 8, NOIR); // ligne 1, colonnes B à G
            p.poser(i * 8 + 7, NOIR); // ligne 8
        }
        p.hash = p.calculer_hash();
        p
    }

    fn vide() -> Self {
        Plateau { cases: [0; 64], noirs: 0, blancs: 0, hash: 0, pile_coups: Vec::with_capacity(1024), pile_pris: Vec::with_capacity(1024) }
    }

    /// Plateau décrit par 64 caractères X (noir), O (blanc) ou . (vide), case = colonne * 8 + ligne.
    pub fn depuis(cases: &str) -> Self {
        let mut p = Plateau::vide();
        for (s, ch) in cases.bytes().enumerate() {
            match ch {
                b'X' => p.poser(s, NOIR),
                b'O' => p.poser(s, BLANC),
                _ => {}
            }
        }
        p.hash = p.calculer_hash();
        p
    }

    fn poser(&mut self, s: usize, couleur: i8) {
        self.cases[s] = couleur;
        if couleur == NOIR { self.noirs |= 1 << s } else { self.blancs |= 1 << s }
    }

    /// Empreinte recalculée depuis zéro.
    pub fn calculer_hash(&self) -> u64 {
        let mut h = 0;
        for s in 0..64 {
            if self.cases[s] != 0 {
                h ^= ZOBRIST[indice(self.cases[s])][s];
            }
        }
        h
    }

    #[inline]
    pub fn hash(&self) -> u64 {
        self.hash
    }

    #[inline]
    pub fn pions(&self, couleur: i8) -> u64 {
        if couleur == NOIR { self.noirs } else { self.blancs }
    }

    #[inline]
    pub fn contenu(&self, s: usize) -> i8 {
        self.cases[s]
    }

    /// Range dans coups[] les coups légaux de cette couleur et renvoie leur nombre.
    pub fn coups(&self, couleur: i8, coups: &mut [i32]) -> usize {
        let amis = self.pions(couleur);
        let occupe = self.noirs | self.blancs;
        let mut n = 0;
        let mut reste = amis;
        while reste != 0 {
            let s = reste.trailing_zeros() as i32;
            reste &= reste - 1;
            let (c, l) = (s >> 3, s & 7);
            for d in 0..4 {
                let pas = (occupe & LIGNE[d][s as usize]).count_ones() as i32;
                for sens in [-1, 1] {
                    let (dc, dl) = (sens * DC[d], sens * DL[d]);
                    if !dedans(c + pas * dc, l + pas * dl) {
                        continue;
                    }
                    let delta = 8 * dc + dl;
                    let arrivee = s + pas * delta;
                    if (amis >> arrivee) & 1 != 0 {
                        continue;
                    }
                    let mut libre = true;
                    let mut k = 1;
                    while k < pas && libre {
                        libre = self.cases[(s + k * delta) as usize] != -couleur;
                        k += 1;
                    }
                    if libre {
                        coups[n] = s << 6 | arrivee;
                        n += 1;
                    }
                }
            }
        }
        n
    }

    /// Joue le coup (départ * 64 + arrivée) ; annuler() le défait.
    #[inline]
    pub fn jouer(&mut self, coup: i32) {
        let (de, vers) = ((coup >> 6) as usize, (coup & 63) as usize);
        let (pion, pris) = (self.cases[de], self.cases[vers]);
        let (bit_de, bit_vers) = (1u64 << de, 1u64 << vers);
        if pris != 0 {
            self.hash ^= ZOBRIST[indice(pris)][vers];
            if pris == NOIR { self.noirs ^= bit_vers } else { self.blancs ^= bit_vers }
        }
        self.hash ^= ZOBRIST[indice(pion)][de] ^ ZOBRIST[indice(pion)][vers];
        if pion == NOIR { self.noirs ^= bit_de | bit_vers } else { self.blancs ^= bit_de | bit_vers }
        self.cases[vers] = pion;
        self.cases[de] = 0;
        self.pile_coups.push(coup);
        self.pile_pris.push(pris);
    }

    /// Annule le dernier coup joué.
    #[inline]
    pub fn annuler(&mut self) {
        let coup = self.pile_coups.pop().expect("aucun coup à annuler");
        let pris = self.pile_pris.pop().unwrap();
        let (de, vers) = ((coup >> 6) as usize, (coup & 63) as usize);
        let pion = self.cases[vers];
        let (bit_de, bit_vers) = (1u64 << de, 1u64 << vers);
        self.hash ^= ZOBRIST[indice(pion)][de] ^ ZOBRIST[indice(pion)][vers];
        if pion == NOIR { self.noirs ^= bit_de | bit_vers } else { self.blancs ^= bit_de | bit_vers }
        self.cases[de] = pion;
        self.cases[vers] = pris;
        if pris != 0 {
            self.hash ^= ZOBRIST[indice(pris)][vers];
            if pris == NOIR { self.noirs ^= bit_vers } else { self.blancs ^= bit_vers }
        }
    }

    /// Vrai si tous les pions de cette couleur forment un seul groupe.
    #[inline]
    pub fn relie(&self, couleur: i8) -> bool {
        let p = self.pions(couleur);
        p != 0 && groupe(p & p.wrapping_neg(), p) == p
    }

    pub fn fin_de_partie(&self) -> bool {
        self.relie(NOIR) || self.relie(BLANC)
    }

    pub fn coups_possibles(&self, couleur: i8) -> Vec<String> {
        let mut t = [0i32; 128];
        let n = self.coups(couleur, &mut t);
        t[..n].iter().map(|&c| ecrire_coup(c)).collect()
    }

    pub fn play(&mut self, coup: &str) {
        self.jouer(lire_coup(coup));
    }
}

/// Les 8 voisins de chaque case de x, et x lui-même, par décalages de bits.
#[inline]
fn etendre(x: u64) -> u64 {
    const PAS_LIGNE_1: u64 = 0xFEFEFEFEFEFEFEFE;
    const PAS_LIGNE_8: u64 = 0x7F7F7F7F7F7F7F7F;
    let x = x | ((x << 1) & PAS_LIGNE_1) | ((x >> 1) & PAS_LIGNE_8);
    x | (x << 8) | (x >> 8)
}

/// Le groupe de pions voisins (8-connexité) qui contient les pions de depart, parmi pions.
#[inline]
pub fn groupe(depart: u64, pions: u64) -> u64 {
    let mut g = depart;
    loop {
        let avant = g;
        g = etendre(g) & pions;
        if g == avant {
            return g;
        }
    }
}

/// "B1-D3" -> entier.
pub fn lire_coup(coup: &str) -> i32 {
    let b = coup.as_bytes();
    let de = (b[0] - b'A') as i32 * 8 + (b[1] - b'1') as i32;
    let vers = (b[3] - b'A') as i32 * 8 + (b[4] - b'1') as i32;
    de << 6 | vers
}

/// entier -> "B1-D3".
pub fn ecrire_coup(coup: i32) -> String {
    let nom = |s: i32| format!("{}{}", (b'A' + (s >> 3) as u8) as char, (b'1' + (s & 7) as u8) as char);
    format!("{}-{}", nom(coup >> 6), nom(coup & 63))
}

pub fn couleur(joueur: &str) -> i8 {
    if joueur.eq_ignore_ascii_case("noir") { NOIR } else { BLANC }
}
