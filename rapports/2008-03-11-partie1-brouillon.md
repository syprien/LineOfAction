<!-- Transcription en Markdown du premier brouillon (document Word du 11 mars 2008, 16h12).
     Texte d'origine conservé, fautes comprises ; les sections vides l'étaient dans l'original. -->

tazzzz
Syprien

# PROJET IA : Lines Of Action

## SOMMAIRE

I. Modélisation du plateau
II. Fin de partie
III. LOA : difficulté et complexité
IV. Coups imparables ?
V. Heuristique
VI. Stratégie globale

## I. Modélisation du plateau

## II. Fin de la partie

Dans le jeu LOA une fin de partie est une chaîne continue de pion qui rassemble tout les pions d'un même joueur. En observant le schéma de fin de partie présentée dans l'énnoncé du projet (fig 3), on peut faire une analogie entre un fin de partie et un graphe comportant tous les pions d'un joueur.
Nous avons donc choisis d'utiliser un algorithme de parcours de graphe pour déterminer si un etat est une fin de partie ou non.

Cependant nous avons dû adapter les algorithmes de parcours vue en cours afin de les adapters à notre structure de données. En effet au première abbord le plateau sous forme de tableau de byte n'est pas vraiment prévu pour être parcourus comme un graphe.
Nous avons créé une fonction permettant de trouver tout les pions adjacents à un autre pion et de la même couleur que celui-ci. Cette fonction prend en paramètre les coordonnées du pion sur le plateau et retourne une liste des pions adjascents.

Grâce à cette méthode, on peut à partir d'un point connaître ces succeceurs. On peut donc, en choissisant un pion au hasard du joueur (dans notre cas le premier pion rencontré lors du parcours du plateau), parcourir le graphe formé par les pions du joueurs et vérifier s'il comporte le même nombre d'éléments que le nombre total de pion du joueur.

## III. LOA : difficulté et complexité

Après avoir analyse un peu le déroulement du jeu nous avons pu constater plusieurs points problématiques. Tout d'abbord le mode de déplacement, en effet à chaque changement d'etat du plateau il est possible qu'un pion ne puisse plus se déplacer de la même manière que à l'etat précédent. Le fait que les possibilités de déplacement évolue avec les changement sur le plateau augmente les difficultés pour créer une heuristique le plus fiable possible. Selon la position du reste des pions du joueur sur le plateau , une même situation «local» pourra passer de très positives à très négatives pour le même joueur. Il faudra donc traités le plateau plus dans sa globalité lors de la mise au poin de l'heuristique.

Comme nous avons vu dans le II, le test pour vérifier si une partie est terminée ou non peut prendre un temps important car il est nécessaire de parcourir un graphe (et précédement pour nous de le construire dynamiquement à partir du tableau de byte) en entier pour chacun des joueurs.

Afin de determiner l'horizon le plus appriorié pour notre algorithme d'IA, nous avons étudier le facteur de branchement de LOA.
Dans le pire des cas, un pion peu être deplacé sur 8 cases différentes. Si on suppose que le joueur garde tous ces pions durant la partie on aboutit à un facteur de branchement pour chaque coup de l'ordre de 128 possibilités. (On est ici dans le pire des cas)

| Profondeur | Nombre de «noeud» visité |
|---|---|
| 1 | 128 |
| 2 | 16384 |
| 3 | ~ 2 . 10^6 |
| 4 | ~ 2 . 10 ^9 |

En tenant compte de la puissance de calcule moyenne des ordinateurs actuels pour avoir un temps de réponse de l'ordre de la seconde, il parrait raisonnable de choisir un horizon entre 2 et 3.

## IV. Coups imparables ?

## V. Heuristique

## VI. Stratégie globale
