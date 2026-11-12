//! L'algo ultime en Rust : le même joueur que Ultime.jar, pour comparer les deux langages.
//!   ultime <machine> <port> <id>        joue une partie contre l'arbitre (comme les .jar)
//!   ultime verifier <fichier>           compare avec les positions exportées par tests/ExportUltime.java
//!   ultime bench <prof> [fils] [bits]  temps d'une recherche à profondeur fixe (comme tests/BenchUltime.java)
//! Nombre de fils (threads) pour jouer : option --fils N, sinon variable ULTIME_FILS, sinon un suffixe
//! « -N » dans le nom de l'exécutable (UltimeRust-16.exe : 16 fils ; l'arbitre ne passe que trois
//! arguments), sinon 1.

mod eval;
mod negascout;
mod ouvertures;
mod plateau;

use std::io::{BufRead, BufReader, Write};
use std::net::TcpStream;
use std::process::ExitCode;
use std::time::Instant;

use negascout::NegaScout;
use ouvertures::Ouvertures;
use plateau::{couleur, JavaRandom, Plateau, NOIR};

/// Taille de la table en partie : 2^22 cases, 64 Mo, partagée par tous les fils.
const BITS_PARTIE: u32 = 22;

/// Nombre de fils : --fils N, sinon ULTIME_FILS, sinon le suffixe « -N » du nom de l'exécutable, sinon 1.
fn nombre_de_fils(args: &mut Vec<String>) -> usize {
    if let Some(i) = args.iter().position(|a| a == "--fils") {
        let n = args.get(i + 1).and_then(|s| s.parse().ok()).expect("--fils <nombre>");
        args.drain(i..i + 2);
        return n;
    }
    if let Some(n) = std::env::var("ULTIME_FILS").ok().and_then(|s| s.parse().ok()) {
        return n;
    }
    let nom = std::path::Path::new(&args[0]).file_stem().map(|s| s.to_string_lossy().to_string()).unwrap_or_default();
    nom.rsplit_once('-').and_then(|(_, n)| n.parse().ok()).unwrap_or(1)
}

fn main() -> ExitCode {
    let mut args: Vec<String> = std::env::args().collect();
    let fils = nombre_de_fils(&mut args);
    match args.get(1).map(|s| s.as_str()) {
        Some("verifier") if args.len() == 3 => verifier(&args[2]),
        Some("bench") if args.len() >= 3 => bench(
            args[2].parse().expect("profondeur"),
            args.get(3).map_or(1, |s| s.parse().expect("fils")),
            args.get(4).map_or(18, |s| s.parse().expect("bits")),
        ),
        _ if args.len() == 4 => match jouer(&args[1], &args[2], &args[3], fils) {
            Ok(()) => ExitCode::SUCCESS,
            Err(e) => {
                println!("Connexion à {}:{} impossible ou interrompue : {}", args[1], args[2], e);
                ExitCode::FAILURE
            }
        },
        _ => {
            println!("Usage : ultime <machine> <port> <id> | ultime verifier <fichier> | ultime bench <profondeur> [fils] [bits] ; option --fils N");
            ExitCode::FAILURE
        }
    }
}

// ------------------------------------------------------------------ le joueur réseau

/// Retire l'en-tête et le '\0' final d'un message de l'arbitre.
fn contenu<'a>(ligne: &'a str, entete: &str) -> &'a str {
    ligne[entete.len()..].trim_end_matches('\0')
}

fn attendre(lignes: &mut impl Iterator<Item = std::io::Result<String>>, entete: &str) -> std::io::Result<String> {
    let ligne = lignes.next().unwrap_or_else(|| Ok(String::new()))?;
    if !ligne.starts_with(entete) {
        return Err(std::io::Error::other(format!("protocole : « {} » attendu, reçu « {} »", entete, ligne)));
    }
    Ok(contenu(&ligne, entete).trim().to_string())
}

/// Le dialogue de net/ClientJeu.java et les choix de ultime/devoirLOA/JoueurReseauIALOA.java.
fn jouer(machine: &str, port: &str, identifiant: &str, fils: usize) -> std::io::Result<()> {
    let flux = TcpStream::connect(format!("{}:{}", machine, port))?;
    flux.set_nodelay(true)?;
    println!("Connexion à {}:{} ({} fil(s), table de 2^{} cases)", machine, port, fils, BITS_PARTIE);
    let mut sortie = flux.try_clone()?;
    let mut lignes = BufReader::new(flux).lines();
    writeln!(sortie, "{}", identifiant)?;
    attendre(&mut lignes, "NOMBRE DE JOUEURS : ")?;
    attendre(&mut lignes, "INDENTIFIANTS JOUEURS : ")?;
    let id = attendre(&mut lignes, "VOTRE IDENTIFIANT : ")?.to_lowercase();
    attendre(&mut lignes, "COMMENCE ")?;

    let livre = Ouvertures::charger();
    let mut p = Plateau::new();
    let mut recherche = NegaScout::new(Some(eval::POIDS), fils, BITS_PARTIE);
    let mut temps: u64 = 300_000;
    let mut disqualification = false;
    while !disqualification && !p.fin_de_partie() {
        let Some(ligne) = lignes.next() else {
            println!("[Arbitre] connexion fermée");
            return Ok(());
        };
        let ligne = ligne?;
        println!("[Arbitre] {}", ligne.replace('\0', ""));
        if ligne.starts_with("MOUVEMENT_ILLEGAL ") {
            disqualification = true;
        } else if ligne.starts_with("MOUVEMENT ") {
            let m: Vec<&str> = contenu(&ligne, "MOUVEMENT ").splitn(2, " : ").collect();
            p.play(m[1].trim());
        } else if ligne.starts_with("A VOUS DE JOUER") {
            let t = ligne[ligne.rfind(':').unwrap() + 1..].replace('\0', "");
            temps = t.trim().parse().unwrap_or(temps);
            let coup = choisir_coup(&mut p, &id, temps, &livre, &mut recherche);
            println!("Je joue : {}", coup);
            write!(sortie, "{}\0\n", coup)?;
            sortie.flush()?;
        }
    }
    for ligne in lignes.take(2) {
        println!("{}", ligne?.replace('\0', ""));
    }
    Ok(())
}

fn choisir_coup(p: &mut Plateau, id: &str, temps: u64, livre: &Ouvertures, recherche: &mut NegaScout) -> String {
    let (debut, noeuds) = (Instant::now(), recherche.noeuds());
    if let Some(c) = livre.coup(p.hash(), id) {
        if p.coups_possibles(couleur(id)).iter().any(|x| x == c) {
            println!("coup {} livre oui", c);
            return c.to_string();
        }
    }
    let budget = 20.max((temps / 25).min(temps.saturating_sub(500)));
    let coup = recherche.meilleur_coup(p, id, budget, 40);
    // Une ligne de trace par coup, au même format que la version Java.
    println!("coup {} profondeur {} noeuds {} score {} temps {} ms livre non",
        coup.as_deref().unwrap_or("null"), recherche.profondeur_atteinte(), recherche.noeuds() - noeuds,
        recherche.score(), debut.elapsed().as_millis());
    coup.unwrap_or_else(|| "A1-A1".to_string())
}

// ------------------------------------------------------------------ vérification et mesure

/// Compare, position par position, avec les valeurs calculées par la version Java.
fn verifier(fichier: &str) -> ExitCode {
    let texte = std::fs::read_to_string(fichier).expect("fichier illisible");
    let (mut positions, mut erreurs, mut recherches) = (0, 0, 0);
    for ligne in texte.lines() {
        let f: Vec<&str> = ligne.split(' ').collect();
        let mut p = Plateau::depuis(f[0]);
        let moi = couleur(f[1]);
        let mut coups = p.coups_possibles(moi);
        coups.sort();
        let coups = if coups.is_empty() { "-".to_string() } else { coups.join(",") };
        let mut obtenu = vec![
            format!("{:x}", p.hash()),
            eval::eval(&p, moi, Some(&eval::POIDS)).to_string(),
            eval::eval(&p, moi, None).to_string(),
            coups,
        ];
        let mut attendu: Vec<String> = f[2..6].iter().map(|s| s.to_string()).collect();
        if f[6] != "-" || f[7] != "-" {
            let mut ns = NegaScout::new(Some(eval::POIDS), 1, 18); // comme Java : un fil, 2^18 cases
            let c = ns.meilleur_coup(&mut p, f[1], 1_000_000, 4).unwrap_or_else(|| "-".to_string());
            obtenu.push(format!("{} {}", c, ns.noeuds()));
            attendu.push(format!("{} {}", f[6], f[7]));
            recherches += 1;
        }
        positions += 1;
        if obtenu != attendu {
            erreurs += 1;
            if erreurs <= 3 {
                println!("différence en position {} :\n  java = {:?}\n  rust = {:?}", positions, attendu, obtenu);
            }
        }
    }
    let livre = Ouvertures::charger();
    println!("positions : {}, dont recherches à profondeur 4 : {}, différences : {}, ouvertures chargées : {}",
        positions, recherches, erreurs, livre.len());
    if erreurs == 0 && livre.len() > 0 { ExitCode::SUCCESS } else { ExitCode::FAILURE }
}

/// Les 8 positions de tests/BenchUltime.java (mêmes coups au hasard, même générateur), cherchées à
/// profondeur fixe : avec un fil et une table de 2^18 cases, Rust et Java font exactement le même
/// travail, seul le temps diffère. Avec plusieurs fils (table de 2^bits cases), on mesure le temps
/// pour atteindre la profondeur : les fils d'appoint font du travail en plus, qui accélère le principal.
fn bench(profondeur: i32, fils: usize, bits: u32) -> ExitCode {
    let mut r = JavaRandom::new(7);
    let mut positions = Vec::new();
    for k in 0..8 {
        let mut p = Plateau::new();
        let mut trait_ = NOIR;
        for _ in 0..4 + 2 * k {
            let l = p.coups_possibles(trait_);
            let c = l[r.next_int(l.len() as i32) as usize].clone();
            p.play(&c);
            trait_ = -trait_;
        }
        positions.push((p, if trait_ == NOIR { "noir" } else { "blanc" }));
    }
    for passe in 1..=2 {
        // Les tables sont créées avant de lancer le chronomètre (64 Mo à mettre à zéro, ça se voit).
        let mut recherches: Vec<NegaScout> = (0..8).map(|_| NegaScout::new(Some(eval::POIDS), fils, bits)).collect();
        let (debut, mut noeuds, mut choix) = (Instant::now(), 0, String::new());
        for ((p, joueur), ns) in positions.iter_mut().zip(recherches.iter_mut()) {
            choix += &ns.meilleur_coup(p, joueur, 1_000_000, profondeur).unwrap();
            choix += " ";
            noeuds += ns.noeuds();
        }
        let ms = debut.elapsed().as_secs_f64() * 1000.0;
        println!("passe {} : {:.0} ms, {} noeuds, {:.0} noeuds/ms  coups : {}", passe, ms, noeuds, noeuds as f64 / ms, choix);
    }
    ExitCode::SUCCESS
}
