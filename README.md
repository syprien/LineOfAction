# Lines of Action — un projet d'IA de 2008, relu en 2026

En 2008, deux étudiants de 3e année, **Syprien** et **tazzzz**, ont écrit en Java un joueur artificiel pour
[Lines of Action](https://fr.wikipedia.org/wiki/Lines_of_Action) dans le cadre du module « Introduction à
l'IA ». Le projet a été rendu le 9 mai 2008 à 3h33 du matin, et l'IA a joué un tournoi contre celles des
autres binômes.

En 2026, le code a été retrouvé, relu, corrigé et remis en compétition. Ce dépôt raconte toute l'histoire
commit par commit : les versions de 2008 sont reconstituées à partir de l'archive et des mails de l'époque,
avec leurs dates d'origine. Le récit complet est dans [HISTOIRE.md](HISTOIRE.md).

## Les étapes

Les étapes de 2008 antérieures au rendu ne contiennent que les fichiers envoyés par mail ce jour-là : seules les étiquettes à partir de `v2008-rendu` compilent.

| Étiquette | Date | Contenu |
|---|---|---|
| `v2008-03-brouillon` | 11 mars 2008 | premier brouillon du rapport de la première partie |
| `v2008-03-plateau` | 12 mars 2008 | premier jet du plateau |
| `v2008-05-negascout` | 7 mai 2008 | premier jet de NegaScout |
| `v2008-05-concentration` | 8 mai 2008 | le critère de concentration, envoyé par mail |
| `v2008-05-plateau` | 8 mai 2008 | le plateau « qui ne devrait plus faire de coups illégaux » |
| `v2008-05-heuristique` | 9 mai 2008 | brouillon de la partie Heuristique du rapport |
| `v2008-rendu` | 9 mai 2008 | **le projet rendu** |
| `v2026-corrige` | 2026 | les bugs corrigés, chacun expliqué par un commentaire `CORRECTIF (2026)` |
| `v2026-arbitre` | 2026 | un arbitre pour rejouer le tournoi, et les scripts de construction |
| `v2026-rival` | 2026 | Rival, une IA écrite par Claude Haiku avec la même limite de taille |
| `v2026-histoire` | 2026 | ce README et l'histoire du projet |
| `v2026-ultime` | 2026 | l'algo ultime : l'évaluation de 2008 avec une recherche moderne et une bibliothèque d'ouvertures |
| `v2026-ultime-rapide` | 2026 | l'algo ultime avec un plateau rapide : bitboards, coups en entiers, jouer puis annuler |
| `v2026-ultime-poids` | 2026 | les poids des critères ajustés par sélection naturelle, comme le proposait le rapport |

Pour voir toutes les corrections, avec leurs explications :

```bash
git diff v2008-rendu v2026-corrige
```

## Construire et jouer

Il faut Java 11 ou plus (`javac` et `jar` dans le PATH). Avec [mise](https://mise.jdx.dev), `mise install` installe
la version indiquée dans `mise.toml`.

```bash
./build.sh                  # ou .\build.ps1 sous Windows
cd build/tournoi
java -jar Arbitre.jar --j1 TazzzzSyprien.jar --j2 Rival.jar --parties 4
```

L'arbitre lance les deux joueurs, inverse les couleurs à chaque partie et affiche le bilan. Options utiles :
`--temps <ms>` (temps par joueur, 5 minutes par défaut), `--plateau` (affiche le plateau après chaque coup).

Pour rejouer la version de 2008 contre la version corrigée :

```bash
./build.sh v2008-rendu
cd build/tournoi
java -jar Arbitre.jar --j1 TazzzzSyprien.jar --j2 TazzzzSyprien_v2008-rendu.jar --parties 4
```

On peut aussi faire comme en 2008 : `java -jar Arbitre.jar --port 1234`, puis lancer chaque joueur à la main
avec `java -jar TazzzzSyprien.jar localhost 1234 TazzzzSyprien`.

## Organisation du code

| Dossier | Rôle |
|---|---|
| `moteur/` | le plateau : règles, coups possibles, fin de partie |
| `jeux/alg/` | les algorithmes de recherche : Minimax, NegaAB, NegaScout |
| `ia/` | l'heuristique (l'évaluation d'une position) |
| `devoirLOA/` | le joueur réseau : gestion du temps, choix du coup |
| `net/` | le client réseau (réécrit en 2026, l'original était fourni par l'enseignant) |
| `arbitre/` | l'arbitre du tournoi (2026) |
| `rival/` | l'IA rivale écrite par Claude Haiku (2026) |
| `ultime/` | l'algo ultime (2026) : produit `Ultime.jar` |
| `tests/` | auto-parties, mesures de temps, vérification croisée des règles (2026) |
| `rapports/` | les rapports de 2008, transcrits en Markdown |

## Licence

MIT, voir [LICENSE](LICENSE).
