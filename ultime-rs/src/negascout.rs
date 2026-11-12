//! La recherche d'Ultime (ultime/jeux/alg/NegaScout.java), portée en Rust : approfondissement
//! itératif, table de transposition, ordre des coups, sur un seul plateau (jouer / annuler).
//! Même algorithme, dans le même ordre : avec un seul fil et une table de 2^18 cases, Rust et Java
//! explorent exactement les mêmes positions (ultime verifier).
//!
//! Plusieurs fils (threads) : « Young Brothers Wait » (Feldmann, vers 1990), le long de la ligne
//! principale. Règle : on n'explore les jeunes frères en parallèle qu'une fois l'aîné (le premier
//! coup) cherché. L'aîné donne la borne alpha (c'est le pari de NegaScout : le premier coup est sans
//! doute le meilleur), et les jeunes frères n'ont plus qu'à vérifier, avec une fenêtre nulle, qu'ils
//! ne font pas mieux. La règle est appliquée à la racine, puis dans l'aîné de la racine, puis dans
//! son aîné, etc. (aine()) : à chaque étage, tous les fils se partagent les jeunes frères (freres()).
//! La table de transposition est partagée, sans verrou.
//!
//! Essais abandonnés, mesurés sur ultime bench 8 :
//!  - « Lazy SMP » (tous les fils cherchent tout l'arbre, un sur deux une profondeur plus loin) :
//!    aucun gain. Notre ordre des coups est déterministe, les fils parcourent l'arbre dans le même
//!    ordre, et un fil plus profond, donc plus lent par branche, est aussitôt dépassé par le
//!    principal : il n'a jamais fini une branche avant que le principal n'y arrive ;
//!  - partage à la racine seulement : plafonne à x2,9, car l'aîné de la racine, cherché par un seul
//!    fil, pèse un tiers du travail (loi d'Amdahl).

use std::sync::atomic::{AtomicBool, AtomicI32, AtomicU64, AtomicUsize, Ordering::Relaxed};
use std::sync::Mutex;
use std::time::{Duration, Instant};

use crate::eval::{self, VICTOIRE};
use crate::plateau::{couleur, ecrire_coup, Plateau, BLANC};

const INFINI: i32 = 1_000_000;
const GAGNE: i32 = VICTOIRE / 2;
const EXACT: i32 = 0;
const AU_MOINS: i32 = 1;
const AU_PLUS: i32 = 2;
const TRAIT_BLANC: u64 = 0x5DEECE66Du64.wrapping_mul(0x9E3779B97F4A7C15);
const PLIS: usize = 64;

#[derive(Clone, Copy)]
struct Entree {
    profondeur: i32,
    valeur: i32,
    genre: i32,
    meilleur: i32,
}

impl Entree {
    /// Une entrée tient dans 64 bits : valeur (32), profondeur (8), genre (2), meilleur coup (12).
    fn emballer(self) -> u64 {
        (self.valeur as u32 as u64)
            | (self.profondeur as u64 & 0xFF) << 32
            | (self.genre as u64 & 3) << 40
            | (self.meilleur as u64 & 0xFFF) << 42
    }

    fn deballer(d: u64) -> Entree {
        Entree {
            valeur: d as u32 as i32,
            profondeur: (d >> 32 & 0xFF) as i32,
            genre: (d >> 40 & 3) as i32,
            meilleur: (d >> 42 & 0xFFF) as i32,
        }
    }
}

/// Table de transposition partagée entre les fils, sans verrou. Chaque case contient deux entiers
/// atomiques : (clé XOR données) et (données). Si deux fils écrivent la même case en même temps, les
/// deux moitiés peuvent venir de deux écritures différentes ; le XOR ne redonne alors plus la clé et
/// l'entrée est simplement ignorée, comme une case vide. Pas de verrou, donc personne n'attend.
pub struct Table {
    cases: Vec<[AtomicU64; 2]>,
    masque: u64,
}

impl Table {
    /// Une table de 2^bits cases (16 octets chacune : 2^22 cases = 64 Mo).
    pub fn new(bits: u32) -> Self {
        Table { cases: (0..1usize << bits).map(|_| [AtomicU64::new(0), AtomicU64::new(0)]).collect(), masque: (1 << bits) - 1 }
    }

    /// L'entrée de cette position, si la case la contient. Comme en Java, une case jamais écrite
    /// (tout à zéro) correspond à la clé 0.
    fn lire(&self, cle: u64) -> Option<Entree> {
        let c = &self.cases[(cle & self.masque) as usize];
        let d = c[1].load(Relaxed);
        if c[0].load(Relaxed) ^ d == cle { Some(Entree::deballer(d)) } else { None }
    }

    fn ecrire(&self, cle: u64, e: Entree) {
        let c = &self.cases[(cle & self.masque) as usize];
        let d = e.emballer();
        c[0].store(cle ^ d, Relaxed);
        c[1].store(d, Relaxed);
    }
}


pub struct NegaScout {
    table: Table,
    poids: Option<[i32; 5]>,
    fils: usize,
    noeuds: u64,
    score_racine: i32,
    profondeur_atteinte: i32,
}

impl NegaScout {
    /// Une recherche avec fils threads et une table de 2^bits cases.
    pub fn new(poids: Option<[i32; 5]>, fils: usize, bits: u32) -> Self {
        NegaScout { table: Table::new(bits), poids, fils: fils.max(1), noeuds: 0, score_racine: 0, profondeur_atteinte: 0 }
    }

    /// Profondeur de la dernière itération terminée, et son score (pour la trace des coups).
    pub fn profondeur_atteinte(&self) -> i32 {
        self.profondeur_atteinte
    }

    pub fn score(&self) -> i32 {
        self.score_racine
    }

    /// Positions explorées depuis la création, tous fils confondus.
    pub fn noeuds(&self) -> u64 {
        self.noeuds
    }

    /// Le meilleur coup pour joueur en au plus budget_ms millisecondes, sans dépasser la profondeur maxi.
    pub fn meilleur_coup(&mut self, p: &mut Plateau, joueur: &str, budget_ms: u64, maxi: i32) -> Option<String> {
        let fin = Instant::now() + Duration::from_millis(budget_ms);
        let stop = AtomicBool::new(false);
        let moi = couleur(joueur);
        let mut racine = [0i32; 128];
        let n = p.coups(moi, &mut racine);
        if n == 0 {
            return None;
        }
        let mut choix = racine[0];
        self.profondeur_atteinte = 0;
        let mut noeuds = 0;
        let mut principal = Fil::new(&self.table, &stop, fin, self.poids);
        for d in 1..=maxi {
            let (m, score, noeuds_aides) = racine_partagee(self.fils, &mut principal, p, moi, d, &racine[..n]);
            noeuds += noeuds_aides;
            if m == 0 {
                break; // profondeur interrompue : on garde la précédente
            }
            choix = m;
            self.profondeur_atteinte = d;
            self.score_racine = score;
            let k = racine.iter().position(|&c| c == m).unwrap();
            racine.copy_within(0..k, 1); // le meilleur coup est exploré en premier ensuite
            racine[0] = m;
            if score >= GAGNE || score <= -GAGNE {
                break; // issue certaine
            }
        }
        self.noeuds += noeuds + principal.noeuds;
        Some(ecrire_coup(choix))
    }
}

/// Profondeur restante à partir de laquelle on partage les jeunes frères d'un aîné : en dessous, lancer
/// les fils coûterait plus cher que le travail lui-même.
const SEUIL_PARTAGE: i32 = 3;

/// Une itération à la profondeur d : (meilleur coup, ou 0 si elle a été interrompue ; score ; nœuds
/// explorés par les fils d'appoint).
fn racine_partagee(fils: usize, f: &mut Fil, p: &mut Plateau, moi: i8, d: i32, coups: &[i32]) -> (i32, i32, u64) {
    // L'aîné, avec une fenêtre complète : il donne la borne alpha. Avec plusieurs fils, on descend
    // dans l'aîné pour y partager aussi les jeunes frères (voir aine()).
    p.jouer(coups[0]);
    let (v, mut aides) = if fils > 1 && d - 1 >= SEUIL_PARTAGE {
        let (v, a) = aine(fils, f, p, -moi, d - 1, 1);
        (-v, a)
    } else {
        (-f.recherche(p, d - 1, -INFINI, INFINI, -moi, 1), 0)
    };
    p.annuler();
    if f.arret {
        return (0, 0, aides);
    }
    if fils == 1 || coups.len() < 3 {
        // Un seul fil : exactement la boucle de la version Java.
        let (mut alpha, mut choix) = (v, coups[0]);
        for &c in &coups[1..] {
            p.jouer(c);
            let mut v = -f.recherche(p, d - 1, -alpha - 1, -alpha, -moi, 1);
            if v > alpha && !f.arret {
                v = -f.recherche(p, d - 1, -INFINI, -alpha, -moi, 1);
            }
            p.annuler();
            if f.arret {
                return (0, 0, 0);
            }
            if v > alpha {
                alpha = v;
                choix = c;
            }
        }
        return (choix, alpha, 0);
    }
    let (alpha, choix, a) = freres(fils, f, p, moi, d, 0, coups, v);
    aides += a;
    if f.arret {
        return (0, 0, aides);
    }
    (choix, alpha, aides)
}

/// Un aîné de la ligne principale (fenêtre complète, joueur au trait, profondeur restante d) :
/// on cherche d'abord SON aîné, en descendant de la même façon, puis on partage ses jeunes frères
/// entre tous les fils. Le long de cette ligne, la fenêtre va de -INFINI à +INFINI : aucune coupure
/// bêta n'y est possible, il n'y a donc jamais à arrêter des fils en plein travail.
/// Renvoie (valeur, nœuds explorés par les fils d'appoint).
fn aine(fils: usize, f: &mut Fil, p: &mut Plateau, joueur: i8, d: i32, pli: usize) -> (i32, u64) {
    f.noeuds += 1;
    if f.noeuds & 127 == 0 && (Instant::now() > f.fin || f.stop.load(Relaxed)) {
        f.arret = true;
    }
    if f.arret {
        return (0, 0);
    }
    if p.relie(-joueur) {
        return (-VICTOIRE - d, 0);
    }
    if p.relie(joueur) {
        return (VICTOIRE + d, 0);
    }
    let k = p.hash() ^ if joueur == BLANC { TRAIT_BLANC } else { 0 };
    let mut coup_memorise = 0;
    if let Some(e) = f.table.lire(k) {
        coup_memorise = e.meilleur;
        if e.profondeur >= d && e.genre == EXACT {
            return (e.valeur, 0); // avec une fenêtre complète, seule une valeur exacte suffit
        }
    }
    let n = p.coups(joueur, &mut f.coups[pli]);
    if n == 0 {
        return (eval::eval(p, joueur, f.poids.as_ref()), 0);
    }
    ordonner(&mut f.coups[pli], &mut f.notes[pli], n, coup_memorise, p);
    let coups: Vec<i32> = f.coups[pli][..n].to_vec(); // copiés : les autres fils vont les lire

    p.jouer(coups[0]);
    let (v, mut aides) = if d - 1 >= SEUIL_PARTAGE {
        let (v, a) = aine(fils, f, p, -joueur, d - 1, pli + 1);
        (-v, a)
    } else {
        (-f.recherche(p, d - 1, -INFINI, INFINI, -joueur, pli + 1), 0)
    };
    p.annuler();
    if f.arret {
        return (0, aides);
    }
    let (alpha, choix) = if n < 2 {
        (v, coups[0])
    } else {
        let (alpha, choix, a) = freres(fils, f, p, joueur, d, pli, &coups, v);
        aides += a;
        (alpha, choix)
    };
    if f.arret {
        return (0, aides);
    }
    f.table.ecrire(k, Entree { profondeur: d, valeur: alpha, genre: EXACT, meilleur: choix });
    (alpha, aides)
}

/// Les jeunes frères coups[1..], partagés entre fils threads, une fois l'aîné cherché (valeur
/// v_aine) : chaque fil prend le suivant dans la file, le vérifie avec une fenêtre nulle contre
/// l'alpha commun, et le recherche en entier s'il le dépasse. Renvoie (alpha, meilleur coup, nœuds
/// explorés par les fils d'appoint) ; f.arret est levé si le temps a manqué.
#[allow(clippy::too_many_arguments)]
fn freres(fils: usize, f: &mut Fil, p: &mut Plateau, joueur: i8, d: i32, pli: usize, coups: &[i32], v_aine: i32) -> (i32, i32, u64) {
    let suivant = AtomicUsize::new(1);
    let alpha_commun = AtomicI32::new(v_aine);
    let meilleur = Mutex::new((v_aine, coups[0], 0usize)); // (score, coup, rang dans la liste)
    let interrompu = AtomicBool::new(false);
    let travail = |f: &mut Fil, q: &mut Plateau| loop {
        let k = suivant.fetch_add(1, Relaxed);
        if k >= coups.len() {
            return;
        }
        let a = alpha_commun.load(Relaxed);
        q.jouer(coups[k]);
        let mut v = -f.recherche(q, d - 1, -a - 1, -a, -joueur, pli + 1);
        if v > a && !f.arret {
            v = -f.recherche(q, d - 1, -INFINI, -a, -joueur, pli + 1);
        }
        q.annuler();
        if f.arret {
            interrompu.store(true, Relaxed);
            return;
        }
        if v > a {
            let mut m = meilleur.lock().unwrap();
            // À score égal, le coup le mieux classé gagne, comme dans la boucle à un fil.
            if v > m.0 || (v == m.0 && k < m.2) {
                *m = (v, coups[k], k);
                alpha_commun.fetch_max(v, Relaxed);
            }
        }
    };
    let (table, stop, fin, poids) = (f.table, f.stop, f.fin, f.poids);
    let aides = std::thread::scope(|s| {
        let aides: Vec<_> = (1..fils)
            .map(|_| {
                let mut q = p.clone();
                let travail = &travail;
                s.spawn(move || {
                    let mut g = Fil::new(table, stop, fin, poids);
                    travail(&mut g, &mut q);
                    g.noeuds
                })
            })
            .collect();
        travail(f, p);
        // Rust impose d'attendre les fils (join) avant la fin du bloc scope : ils empruntent la
        // table, la file et le verrou, qui n'existent que dans ce bloc.
        aides.into_iter().map(|a| a.join().unwrap()).sum::<u64>()
    });
    if interrompu.load(Relaxed) {
        f.arret = true; // un fil d'appoint a manqué de temps : l'itération est incomplète
    }
    let (score, coup, _) = *meilleur.lock().unwrap();
    (score, coup, aides)
}

/// Un fil de recherche : sa copie du plateau et ses tableaux de coups ; la table est partagée.
struct Fil<'a> {
    table: &'a Table,
    stop: &'a AtomicBool,
    fin: Instant,
    poids: Option<[i32; 5]>,
    coups: Vec<[i32; 128]>,
    notes: Vec<[i32; 128]>,
    noeuds: u64,
    arret: bool,
}

impl<'a> Fil<'a> {
    fn new(table: &'a Table, stop: &'a AtomicBool, fin: Instant, poids: Option<[i32; 5]>) -> Self {
        Fil { table, stop, fin, poids, coups: vec![[0; 128]; PLIS], notes: vec![[0; 128]; PLIS], noeuds: 0, arret: false }
    }

    fn recherche(&mut self, p: &mut Plateau, d: i32, mut alpha: i32, beta: i32, joueur: i8, pli: usize) -> i32 {
        self.noeuds += 1;
        if self.noeuds & 127 == 0 && (Instant::now() > self.fin || self.stop.load(Relaxed)) {
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
        let mut coup_memorise = 0;
        if let Some(e) = self.table.lire(k) {
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

        let genre = if alpha <= alpha_initial { AU_PLUS } else if alpha >= beta { AU_MOINS } else { EXACT };
        self.table.ecrire(k, Entree { profondeur: d, valeur: alpha, genre, meilleur: choix });
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
