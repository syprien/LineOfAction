# L'histoire du projet

## 2008 : le projet

Le module « Introduction à l'IA » demandait d'écrire un joueur de Lines of Action capable de jouer en
réseau contre ceux des autres binômes, lors d'un tournoi piloté par un serveur. Le projet se rendait en deux
parties : d'abord le plateau et ses règles, puis l'IA.

Les commits de 2008 de ce dépôt ont été **reconstitués en 2026** à partir de l'archive du rendu et des
pièces jointes des mails échangés entre les deux étudiants. Leurs dates sont celles des mails ; leur contenu
est celui des fichiers d'origine, seulement convertis en UTF-8 et pseudonymisés.

### Chronologie

| Date | Qui | Quoi |
|---|---|---|
| 8 février 2008 | l'enseignant | le cadre de jeu du cours (`iia.jeux.alg` : `AlgoJeu`, `CoupHeuri`, `Heuristique`, `Minimax`), fourni compilé |
| 11 mars, 16h12 | tazzzz | premier brouillon du rapport : la fin de partie vue comme un parcours de graphe, et une estimation du facteur de branchement (128 au pire), d'où un horizon de 2 ou 3 coups |
| 12 mars, 12h10 | tazzzz | premier `Plateau.java` : fin de partie et sauvegarde écrites, `estValide` et `play` encore vides (« TODO Syprien »), et un `;` manquant |
| 3 mai | Syprien | « mon idée de la plus grande chaîne risque de bloquer en début de partie (le PC ne voulant pas casser sa chaîne de départ) » : c'est l'origine des coups aléatoires en ouverture |
| 7 mai, 21h37 | tazzzz | premier NegaScout, « qui semble marcher », avec un temps de calcul « vraiment ridicule » |
| 8 mai, 16h52 | Syprien | le code du critère de concentration, envoyé par mail |
| 8 mai, 18h39 | tazzzz | le plateau retravaillé : « il ne devrait plus faire de coups illégaux (enfin j'espère…) » |
| 9 mai, 00h40 | Syprien | le brouillon de la partie Heuristique du rapport |
| 9 mai, 00h36 → 03h29 | les deux | la nuit du rendu : `Minimax` (00h36), `NegaAB` (00h41), `Plateau` (01h39), `HeuristiqueLoa` (02h21), `NegaScout` (02h24), le joueur (02h28), le rapport PDF (03h29) |
| 9 mai, 03h33 | tazzzz | envoi du rendu |

### Qui a fait quoi

- **Le plateau** (`moteur/Plateau.java`) est un travail commun : la structure, la fin de partie et la
  sauvegarde viennent de tazzzz (version du 12 mars), la validation des coups était confiée à Syprien, puis
  tazzzz a retravaillé le fichier le 8 mai pour éliminer les coups illégaux.
- **La recherche** (`jeux/alg/` : Minimax, NegaAB, NegaScout) et le joueur réseau sont de tazzzz.
  `Minimax.java` est une adaptation du `Minimax` du cadre fourni par le cours (mêmes champs, mêmes méthodes).
- **L'heuristique** (`ia/HeuristiqueLoa.java`) : la partie au-dessus de la ligne de séparation (`eval`, la
  chaîne la plus longue) et le critère de concentration sont de Syprien ; les derniers critères (`centre`,
  `nbPion`, `nb_voisin`) ont été ajoutés pendant la nuit du rendu.
- **La stratégie de temps** (coups aléatoires en ouverture, « mode panique » à la fin, 5 % de coups au hasard)
  et le choix des critères ont été décidés ensemble ; Syprien a rédigé la partie Heuristique du rapport.

Les fichiers `.class` du cadre de cours ne sont pas publiés, ni le client réseau d'origine
(`net/ClientJeu.java`, `net/JoueurReseau.java`, `moteur/Partie1.java`), fourni par l'enseignant : ces trois
fichiers ont été réécrits en 2026 d'après le protocole, avec les mêmes noms et les mêmes méthodes.

## 2026 : la relecture

### Les bugs, et ce qu'ils apprennent

Chaque correction est expliquée dans le code par un commentaire `CORRECTIF (2026)`
(`git diff v2008-rendu v2026-corrige`). Les plus importantes :

| Bug | Effet | Leçon |
|---|---|---|
| la recherche ne fait jamais jouer l'adversaire (déjà présent le 7 mai) | l'IA planifie comme si elle jouait 3 coups d'affilée | negamax doit alterner les joueurs |
| pas de signe moins à la racine de NegaScout | l'IA choisit le coup le meilleur… pour l'adversaire | en negamax, mon score est l'opposé de celui de l'adversaire |
| `joueur == "BLANC"` | l'IA évalue toujours comme si elle était Noire | `==` compare les objets, pas le texte des chaînes |
| `catch (Exception e) { return 0; }` | les erreurs de l'heuristique deviennent des scores nuls, en silence | ne jamais avaler une exception |
| `Integer.MAX_VALUE` pour une victoire | une position gagnante peut être notée comme la pire | attention aux dépassements d'entier |
| `-Integer.MIN_VALUE` | la fenêtre alpha-bêta est faussée | `-Integer.MIN_VALUE == Integer.MIN_VALUE` en Java |
| deux `ite.next()` dans la même boucle | un pion sur deux est sauté, et parfois une exception | chaque `next()` fait avancer l'itérateur |
| `break` manquant dans un `switch` | le niveau 4 de l'heuristique était identique au niveau 3 | le « fall-through » du `switch` |
| boucles imbriquées pour vérifier une diagonale | des coups diagonaux légaux sont refusés | sur une diagonale, ligne et colonne avancent ensemble |

Le premier NegaScout (7 mai) démarrait la recherche à `prof = profMax` : il s'arrêtait donc immédiatement et
n'explorait aucun coup, quelle que soit la profondeur demandée. C'est ce qui expliquait le temps de calcul
« vraiment ridicule ». Ce bug-là était corrigé dans la version rendue.

Un autre bug est apparu en 2026 seulement, une fois la recherche corrigée : l'heuristique n'était pas à somme
nulle (`centre` et `nbPion` n'étaient comptés que pour un camp), ce que negamax ne supporte pas.

### Le tournoi rejoué

Un arbitre a été reconstitué d'après le protocole attendu par le client réseau (`arbitre/`). Il vérifie les
coups avec ses propres règles, indépendantes du plateau des joueurs.

| Match | Résultat |
|---|---|
| version corrigée contre version de 2008 (2 min par joueur) | **4 – 0** |
| Rival contre version corrigée (5 min par joueur) | **3 – 1** pour Rival |
| Rival contre version corrigée (2 min par joueur) | 1 – 1 |

**Rival** (`rival/`) a été écrit en 2026 par Claude Haiku, avec pour seule consigne les règles, le protocole
et la même limite de taille que le projet de 2008 (1 730 lignes) : il en utilise 314. Il gagne grâce à sa
recherche (approfondissement itératif, table de transposition, ordre des coups) ; l'évaluation de 2008, avec
la concentration et la prudence sur les captures, reste plus riche que la sienne.

En 2008, l'IA a gagné plus de la moitié de ses matchs du tournoi, souvent parce que l'adversaire jouait un coup
illégal : son plateau refusait parfois des coups légaux, mais n'en acceptait jamais d'illégaux.

## L'algo ultime

L'étape `v2026-ultime` (dossier `ultime/`) part du code de 2008 corrigé et garde son évaluation
(concentration, chaîne la plus longue, centralisation, nombre de pions et de voisins). Ce qui change :

- **approfondissement itératif** : NegaScout cherche à profondeur 1, puis 2, puis 3… jusqu'à épuiser le temps
  accordé au coup (1/25 du temps restant), à la place de la profondeur adaptative et du « mode panique » ;
- **table de transposition** indexée par une empreinte de Zobrist ajoutée au plateau ;
- **ordre des coups** : le meilleur coup mémorisé, puis les captures, puis les coups vers le centre ;
- **bibliothèque d'ouvertures**, calculée à l'avance (10 secondes de recherche par position) à la place des
  coups aléatoires : les deux premiers coups sont instantanés ;
- plus de coups au hasard.

| Match (2 min par joueur) | Résultat |
|---|---|
| Ultime contre la version corrigée | **2 – 0** |
| Ultime contre Rival (sans bibliothèque d'ouvertures) | 1 – 3 |

À évaluation égale, la recherche moderne suffit à battre la version corrigée. Elle ne suffit pas contre Rival :
le plateau de 2008 copie toute la position pour chaque coup essayé et manipule les coups sous forme de chaînes,
alors que Rival joue et annule ses coups sur un seul tableau d'entiers ; il explore donc bien plus de positions
dans le même temps. Prochaines étapes : accélérer le plateau, puis ajuster les poids des critères en faisant
s'affronter des variantes, comme le proposait la conclusion du rapport de 2008.

## L'algo ultime, version rapide

L'étape `v2026-ultime-rapide` réécrit le plateau d'Ultime pour la vitesse, sans toucher aux règles ni à
l'évaluation. Ultime passe de 1 451 à 679 lignes.

- **Jouer puis annuler.** La recherche travaille sur un seul plateau : elle joue un coup, explore, puis
  l'annule grâce à une petite pile qui retient le coup et le pion éventuellement pris. Avant, chaque coup essayé
  copiait les 64 cases et recomptait tous les pions.
- **Des coups en entiers.** Une case est numérotée `colonne × 8 + ligne` (A1 = 0, B1 = 8, H8 = 63) et un coup
  vaut `départ × 64 + arrivée` : `B1-D3` devient 8 × 64 + 26 = 538. C'est un nombre à deux chiffres en base 64,
  qu'on relit avec `coup >>> 6` (le départ) et `coup & 63` (l'arrivée). Les chaînes ne servent plus qu'à parler au
  serveur.
- **Des bitboards.** Les pions de chaque couleur sont aussi rangés dans un `long` de 64 bits, un bit par case.
  Compter les pions d'une ligne, qui fixe la longueur d'un déplacement, devient `Long.bitCount(occupé & ligne)`.
  Un groupe de pions voisins se trouve en décalant les bits d'une case dans les 8 directions jusqu'à ce que le
  groupe ne grandisse plus, au lieu du parcours récursif avec des `ArrayList<Couple>`.
- **La même évaluation, au chiffre près.** Les critères de 2008 sont recalculés sur les bitboards ;
  `tests/arbitre/CheckUltimeEval` vérifie sur 23 904 positions qu'ils donnent exactement le même nombre que le
  code de 2008, pour les deux couleurs et les quatre niveaux.

En chemin, un bug de 2008 est apparu : `finDePartie()` s'arrêtait après les deux premiers pions rencontrés,
même s'ils étaient de la même couleur. Sur le plateau de départ, ce sont A2 et A3, deux pions blancs : une
victoire de Noir n'était jamais détectée tant que la colonne A gardait deux pions blancs. Il est corrigé dans
la version corrigée (`moteur/Plateau.java`) comme dans Ultime.

Le nouveau plateau explore 3,1 millions de positions par seconde, contre 159 000 avant, soit environ 19,5 fois
plus.

| Match (2 min par joueur) | Résultat |
|---|---|
| Ultime rapide contre l'ancien Ultime | **4 – 0** |
| Ultime rapide contre la version corrigée | **2 – 0** |
| Ultime rapide contre Rival | 1 – 5 |

La vitesse suffit à écraser l'ancien Ultime, mais pas à battre Rival : à recherche comparable, c'est maintenant
l'évaluation qui fait la différence. Prochaine étape : ajuster les poids des critères en faisant s'affronter des
variantes, comme le proposait la conclusion du rapport de 2008.

## Le terme « pions / voisins » : le code contre le rapport

En mesurant le poids de chaque critère de l'évaluation, une surprise : le dernier terme,
`(ennemis² − amis) / (nombre moyen de voisins)`, pesait en moyenne **dix fois plus** que la chaîne la plus longue,
la concentration et la centralisation réunies. Il décidait presque seul.

Le rapport de 2008 décrit pourtant deux critères **mineurs**, simplement ajoutés aux autres :

- « Nombre de pion » : « il serait mal avisé d'en capturer un trop grand nombre. On compte ici la différence de
  nombre entre les pions amis et ennemi » ;
- « Nombre moyen de voisins » : « C'est une fonction mineure mais elle renforce la concentration ».

Le code, lui, élevait le nombre de pions ennemis au carré (environ 132 au départ) et divisait le tout par le nombre
moyen de voisins. Le rapport raconte que ce terme « grève les performances » mais permettait de gagner contre la
version qui ne l'avait pas. C'était sur le moteur de 2008, où la recherche n'alternait pas les joueurs et où un
simple `else` comptait les cases vides comme des pions ennemis : il mesurait alors tout autre chose.

Le code est considéré comme un bug et corrigé pour faire ce que prévoit le rapport, dans la version corrigée
(`ia/HeuristiqueLoa.java`) comme dans Ultime : `ennemis − amis`, plus `mes voisins moyens − les siens`.

| Match (2 min par joueur) | Avant | Après |
|---|---|---|
| Ultime rapide contre Rival | 1 – 5 | **6 – 0** |
| version corrigée contre Rival | 1 – 1 | 2 – 2 |
| version corrigée contre version de 2008 | 4 – 0 | **2 – 0** |

Les deux IA étant déterministes, les trois parties où Ultime a les noirs sont la même partie rejouée ; celles où il a
les blancs varient avec les temps de réflexion. Avec l'évaluation que décrivait le rapport de 2008, l'algo
ultime bat Rival.
