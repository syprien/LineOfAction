//! La bibliothèque d'ouvertures d'Ultime. Elle n'est pas recopiée : le fichier Java
//! (ultime/devoirLOA/Ouvertures.java) est inclus à la compilation et ses lignes
//! « ajouter(0x...L, "noir", "B1-D3"); » sont relues au démarrage. Les empreintes de Zobrist étant
//! identiques en Rust et en Java, les deux versions jouent la même bibliothèque.

use std::collections::HashMap;

const SOURCE: &str = include_str!("../../ultime/devoirLOA/Ouvertures.java");

pub struct Ouvertures(HashMap<(u64, String), String>);

impl Ouvertures {
    pub fn charger() -> Self {
        let mut table = HashMap::new();
        for ligne in SOURCE.lines() {
            let Some(debut) = ligne.find("ajouter(0x") else { continue };
            let reste = &ligne[debut + "ajouter(0x".len()..];
            let Some(fin) = reste.find("L,") else { continue };
            let hash = u64::from_str_radix(&reste[..fin], 16).expect("empreinte illisible");
            let champs: Vec<&str> = reste.split('"').collect();
            table.insert((hash, champs[1].to_string()), champs[3].to_string());
        }
        Ouvertures(table)
    }

    pub fn coup(&self, hash: u64, joueur: &str) -> Option<&str> {
        self.0.get(&(hash, joueur.to_string())).map(|s| s.as_str())
    }

    pub fn len(&self) -> usize {
        self.0.len()
    }
}
