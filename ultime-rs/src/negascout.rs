//! La recherche d'Ultime (ultime/jeux/alg/NegaScout.java), portée en Rust : approfondissement
//! itératif, table de transposition, ordre des coups, sur un seul plateau (jouer / annuler).
//! Même algorithme, dans le même ordre : à profondeur fixe, Rust et Java explorent exactement les
//! mêmes positions (ultime verifier).

use std::time::{Duration, Instant};

use crate::eval::{self, VICTOIRE};
use crate::plateau::{couleur, ecrire_coup, Plateau, BLANC};

const INFINI: i32 = 1_000_000;
const GAGNE: i32 = VICTOIRE / 2;
const EXACT: i32 = 0;
const AU_MOINS: i32 = 1;
const AU_PLUS: i32 = 2;
const TAILLE: usize = 1 << 18;
const TRAIT_BLANC: u64 = 0x5DEECE66Du64.wrapping_mul(0x9E3779B97F4A7C15);
const PLIS: usize = 64;

#[derive(Clone, Copy, Default)]
struct Entree {
    cle: u64,
    profondeur: i32,
    valeur: i32,
    genre: i32,
    meilleur: i32,
}

pub struct NegaScout {
    table: Vec<Entree>,
    coups: Vec<[i32; 128]>,
    notes: Vec<[i32; 128]>,
    poids: Option<[i32; 5]>,
    fin: Instant,
    noeuds: u64,
    arret: bool,
    score_racine: i32,
    profondeur_atteinte: i32,
}

impl NegaScout {
    pub fn new(poids: Option<[i32; 5]>) -> Self {
        NegaScout {
            table: vec![Entree::default(); TAILLE],
            coups: vec![[0; 128]; PLIS],
            notes: vec![[0; 128]; PLIS],
            poids,
            fin: Instant::now(),
            noeuds: 0,
            arret: false,
            score_racine: 0,
            profondeur_atteinte: 0,
        }
    }

    /// Profondeur de la dernière itération terminée, et son score (pour la trace des coups).
    pub fn profondeur_atteinte(&self) -> i32 {
        self.profondeur_atteinte
    }

    pub fn score(&self) -> i32 {
        self.score_racine
    }

    pub fn noeuds(&self) -> u64 {
        self.noeuds
    }

    /// Le meilleur coup pour joueur en au plus budget_ms millisecondes, sans dépasser la profondeur maxi.
    pub fn meilleur_coup(&mut self, p: &mut Plateau, joueur: &str, budget_ms: u64, maxi: i32) -> Option<String> {
        self.fin = Instant::now() + Duration::from_millis(budget_ms);
        self.arret = false;
        self.profondeur_atteinte = 0;
        let moi = couleur(joueur);
        let mut racine = self.coups[0];
        let n = p.coups(moi, &mut racine);
        if n == 0 {
            return None;
        }
        let mut choix = racine[0];
        for d in 1..=maxi {
            let m = self.racine(p, moi, d, &racine[..n]);
            if m == 0 {
                break; // profondeur interrompue : on garde la précédente
            }
            choix = m;
            self.profondeur_atteinte = d;
            let k = racine.iter().position(|&c| c == m).unwrap();
            racine.copy_within(0..k, 1); // le meilleur coup est exploré en premier ensuite
            racine[0] = m;
            if self.score_racine >= GAGNE || self.score_racine <= -GAGNE {
                break; // issue certaine
            }
        }
        Some(ecrire_coup(choix))
    }

    fn racine(&mut self, p: &mut Plateau, joueur: i8, d: i32, coups: &[i32]) -> i32 {
        let mut alpha = -INFINI;
        let mut choix = 0;
        for (k, &c) in coups.iter().enumerate() {
            p.jouer(c);
            let mut v;
            if k == 0 {
                v = -self.recherche(p, d - 1, -INFINI, -alpha, -joueur, 1);
            } else {
                v = -self.recherche(p, d - 1, -alpha - 1, -alpha, -joueur, 1);
                if v > alpha && !self.arret {
                    v = -self.recherche(p, d - 1, -INFINI, -alpha, -joueur, 1);
                }
            }
            p.annuler();
            if self.arret {
                return 0;
            }
            if k == 0 || v > alpha {
                alpha = v;
                choix = c;
            }
        }
        self.score_racine = alpha;
        choix
    }

    fn recherche(&mut self, p: &mut Plateau, d: i32, mut alpha: i32, beta: i32, joueur: i8, pli: usize) -> i32 {
        self.noeuds += 1;
        if self.noeuds & 127 == 0 && Instant::now() > self.fin {
            self.arret = true;
        }
        if self.arret {
            return 0;
        }
        // Partie finie : l'adversaire vient de jouer ; s'il a relié ses pions, il a gagné.
        if p.relie(-joueur) {
            return -VICTOIRE - d;
        }
        if p.relie(joueur) {
            return VICTOIRE + d;
        }
        if d <= 0 || pli >= PLIS {
            return eval::eval(p, joueur, self.poids.as_ref());
        }

        let k = p.hash() ^ if joueur == BLANC { TRAIT_BLANC } else { 0 };
        let i = (k & (TAILLE as u64 - 1)) as usize;
        let mut coup_memorise = 0;
        let e = self.table[i];
        if e.cle == k {
            coup_memorise = e.meilleur;
            if e.profondeur >= d {
                let v = e.valeur;
                if e.genre == EXACT || (e.genre == AU_MOINS && v >= beta) || (e.genre == AU_PLUS && v <= alpha) {
                    return v;
                }
            }
        }

        // Les coups de ce niveau restent dans self.coups[pli] : on les relit par indice, sans copie.
        let n = p.coups(joueur, &mut self.coups[pli]);
        if n == 0 {
            return eval::eval(p, joueur, self.poids.as_ref());
        }
        ordonner(&mut self.coups[pli], &mut self.notes[pli], n, coup_memorise, p);

        let alpha_initial = alpha;
        let mut b = beta;
        let mut choix = 0;
        for m in 0..n {
            let coup = self.coups[pli][m];
            p.jouer(coup);
            let mut v = -self.recherche(p, d - 1, -b, -alpha, -joueur, pli + 1);
            if m > 0 && v > alpha && v < beta && d > 1 && !self.arret {
                v = -self.recherche(p, d - 1, -beta, -v, -joueur, pli + 1);
            }
            p.annuler();
            if self.arret {
                return 0;
            }
            if v > alpha || choix == 0 {
                if v > alpha {
                    alpha = v;
                }
                choix = coup;
            }
            if alpha >= beta {
                break;
            }
            b = alpha + 1;
        }

        self.table[i] = Entree {
            cle: k,
            profondeur: d,
            valeur: alpha,
            genre: if alpha <= alpha_initial { AU_PLUS } else if alpha >= beta { AU_MOINS } else { EXACT },
            meilleur: choix,
        };
        alpha
    }
}

/// Trie les coups (tri par insertion) : coup mémorisé, puis captures, puis arrivée près du centre.
fn ordonner(c: &mut [i32; 128], note: &mut [i32; 128], n: usize, coup_memorise: i32, p: &Plateau) {
    for k in 0..n {
        let coup = c[k];
        let vers = coup & 63;
        let (col, lig) = (vers >> 3, vers & 7);
        let x = if coup == coup_memorise {
            1000
        } else {
            (if p.contenu(vers as usize) != 0 { 100 } else { 0 }) + 14 - (2 * col - 7).abs() - (2 * lig - 7).abs()
        };
        let mut j = k;
        while j > 0 && note[j - 1] < x {
            note[j] = note[j - 1];
            c[j] = c[j - 1];
            j -= 1;
        }
        note[j] = x;
        c[j] = coup;
    }
}
