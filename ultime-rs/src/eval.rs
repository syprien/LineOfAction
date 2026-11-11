//! L'évaluation d'Ultime (ultime/ia/HeuristiqueLoa.java), portée en Rust : mêmes critères, mêmes
//! nombres. Les arrondis et les divisions entières sont ceux de Java (Math.round sur un float,
//! division tronquée vers zéro, qui est aussi celle de Rust).

use crate::plateau::{groupe, Plateau, VOISINS};

pub const VICTOIRE: i32 = 10000;

/// Les poids choisis par sélection naturelle : chaîne, concentration, centre, pions, voisins.
pub const POIDS: [i32; 5] = [13, 10, 6, 1, 1];

const VALEUR_2008: [i32; 64] = [
    -16, -8, -4, -4, -4, -4, -8, -16,
    -8, 1, 1, 1, 1, 1, 1, -8,
    -2, 1, 6, 6, 6, 6, 1, -4,
    -2, 1, 6, 12, 12, 6, 1, -4,
    -2, 1, 6, 12, 12, 6, 1, -4,
    -2, 1, 6, 6, 6, 6, 1, -4,
    -8, 1, 1, 1, 1, 1, 1, -8,
    -16, -8, -4, -4, -4, -4, -8, -16,
];

/// Table de centralisation réindexée case = colonne * 8 + ligne.
const fn valeurs() -> [i32; 64] {
    let mut t = [0; 64];
    let mut s = 0;
    while s < 64 {
        t[s] = VALEUR_2008[(s & 7) * 8 + (s >> 3)];
        s += 1;
    }
    t
}
static VALEUR: [i32; 64] = valeurs();

/// Évaluation du point de vue de ami ; poids None : la somme de 2008.
pub fn eval(p: &Plateau, ami: i8, poids: Option<&[i32; 5]>) -> i32 {
    let (a, e) = (p.pions(ami), p.pions(-ami));
    let chaine = ligne_max(a) - ligne_max(e);
    let concentration = -evaluation(a) + evaluation(e);
    let centre = centre(a) - centre(e);
    let pions = e.count_ones() as i32 - a.count_ones() as i32;
    let voisins = nb_voisin(a) - nb_voisin(e);
    match poids {
        None => chaine + concentration + centre + pions + voisins,
        Some(w) => w[0] * chaine + w[1] * concentration + w[2] * centre + w[3] * pions + w[4] * voisins,
    }
}

/// Taille du plus grand groupe de pions voisins, ou VICTOIRE si ce groupe contient tous les pions.
fn ligne_max(pions: u64) -> i32 {
    let mut max = 0;
    let mut reste = pions;
    while reste != 0 {
        let g = groupe(reste & reste.wrapping_neg(), pions);
        max = max.max(g.count_ones());
        reste &= !g;
    }
    if max == pions.count_ones() { VICTOIRE } else { max as i32 }
}

/// Math.round(float) de Java : floor(x + 0,5).
fn arrondi_java(x: f32) -> i32 {
    (x + 0.5).floor() as i32
}

/// Concentration : somme des distances au barycentre, moins le minimum incompressible.
fn evaluation(pions: u64) -> i32 {
    let n = pions.count_ones() as i32;
    let (mut sc, mut sl) = (0, 0);
    let mut r = pions;
    while r != 0 {
        let s = r.trailing_zeros() as i32;
        r &= r - 1;
        sc += s >> 3;
        sl += s & 7;
    }
    let (mut bc, mut bl) = (0, 0);
    if n != 0 {
        bc = arrondi_java(sc as f32 / n as f32);
        bl = arrondi_java(sl as f32 / n as f32);
    }
    let mut res = 0;
    let mut r = pions;
    while r != 0 {
        let s = r.trailing_zeros() as i32;
        r &= r - 1;
        res += ((s >> 3) - bc).abs().max(((s & 7) - bl).abs());
    }
    mouv_min(res, n)
}

fn mouv_min(res: i32, pion: i32) -> i32 {
    if pion == 1 {
        res
    } else if pion <= 9 {
        res - pion
    } else {
        res - 8 - 2 * (pion - 8)
    }
}

fn centre(pions: u64) -> i32 {
    let n = pions.count_ones() as i32;
    let mut res = 0;
    let mut r = pions;
    while r != 0 {
        res += VALEUR[r.trailing_zeros() as usize];
        r &= r - 1;
    }
    if n == 0 { 0 } else { res / n }
}

/// Nombre moyen de voisins de la même couleur, par pion.
fn nb_voisin(pions: u64) -> i32 {
    let n = pions.count_ones() as i32;
    let mut res = 0;
    let mut r = pions;
    while r != 0 {
        res += (VOISINS[r.trailing_zeros() as usize] & pions).count_ones() as i32;
        r &= r - 1;
    }
    if n == 0 { 0 } else { res / n }
}
