<!-- Brouillon de la section « Heuristique » du rapport, envoyé par mail par Syprien
     le 9 mai 2008 à 00h40. Texte d'origine, fautes comprises. -->

Les 5 points suivants ont été retenus pour l'heuristique :

* concentration
* centralisation
* nombre de pion
* chaîne la plus longue
* nombre moyen de pion voisins

Les premiers coups ont une importance moindres, les pions ne pouvant
presque que se rapprocher. Ainsi il a été choisi de ne pas calculer
d'heuristique sur les premiers coups et de jouer rapidement au hasard
afin de sauvegarder du temps pour le coeur de la partie.

Là où la machine prenait une dizaine de seconde à analyser une situation
de départ pour laquelle elle n'était pas fait, le mouvement est
instantané.

## Centralisation :

Les pièces situé dans les coins et sur les bords disposes de moins de
possibilités de mouvement, sont facilement blocable par les pions
adverses et inversement ne le gène jamais.

Il s'agit donc de position à éviter pour ses pions.

## Concentration:

Le but du Line-of-Action est de réunir ses pions en une chaine, celle ci
est plus difficilement cassable si il s'agit d'un bloc compact. Ainsi il
vaut mieux se réunir autour d'un même point.

On calcule donc la distance entre chacun des pions et l'équi-barycentre
(centre de gravité du polygone formé par les pions).

La somme traduit le nombre de mouvement case par case avant d'atteindre
une forme concentré.

## Chaine la plus longue :

Il s'agit simplement de calculer la chaîne la plus longue.

## Nombre moyen de voisin :

A mi-chemin entre la concentration et la chaine la plus longue, elle
traduit le nombre de liaisons entre les pions. Plus elles sont
nombreuses, plus il est difficile de casser des chaines et plus on
s'approche d 'un dénouement.

## Nombre de pion :

Le nombre de pion influence la difficulté à tous les rassembler, il
serait mal avisé d'en capturer un trop grand nombre. On compte ici la
différence de nombre entre les pions amis et ennemi.
