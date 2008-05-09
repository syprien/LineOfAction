<!-- Transcription en Markdown du rapport rendu le 9 mai 2008 (PDF de 5 pages, non publié).
     Texte d'origine, fautes comprises ; l'image du plateau est remplacée par un schéma texte. -->

tazzzz
Syprien

09/05/2008

# PROJET IA : *Line Of Action*

Module : Introduction à l'IA

*Enseignant : Prof S. – Chargé de TD : Prof C.*

---

## HEURISTIQUE

Les 5 points suivants ont été retenus pour l'heuristique :

* concentration
* centralisation
* nombre de pion
* chaîne la plus longue
* nombre moyen de pion voisins

Les premiers coups ayant une importance moindres, les pions ne pouvant presque que se rapprocher les un des autres,nous avons choisi de ne pas calculer d'heuristique sur les premiers coups et de jouer rapidement au hasard afin de sauvegarder du temps pour le coeur de la partie.
Là où la machine prenait une dizaine de seconde à analyser une situation de départ pour laquelle elle n'était pas faite, le hasard permet une réponse très rapide. Il permet aussi de varier le comportement de l'IA en debut de partie et ainsi d'éviter les comportements trop répétitifs.

Réciproquement lorsqu'il ne reste plus qu'une dizaine de seconde, l'IA « entre en panique » et n'évalue plus le jeu qu'à une profondeur d'un coup, puis à une seconde de la fin du chronomètre : elle recommence à jouer aléatoirement, s'en remettant à la chance pour gagner plutôt que de perdre par dépassement de temps.

Évidement le premier test est de savoir si le coup est gagnant, si oui : on retourne la valeur '*infini*' et les sous fonctions heuristiques ne sont pas utilisées.

### Centralisation :

Les pièces situé dans les coins et sur les bords disposes de moins de possibilités de mouvement, sont facilement blocable par les pions adverses et inversement ne le gène jamais.
Il s'agit donc de position à éviter pour ses pions.
Ainsi on évalue la position de chacun des pions et on calcule pour le plateau la moyenne (et non la somme) des positions. Ceci pour éviter la prise systématique des pièces centrales de l'adversaire.

### *Concentration:*

Le but du Line-of-Action est de réunir ses pions en une chaine, celle ci est plus difficilement cassable si il s'agit d'un bloc compact. Ainsi il vaut mieux se réunir autour d'un même point.
On calcule donc la distance entre chacun des pions et l'équi-barycentre (centre de gravité du polygone formé par les pions).
La somme traduit le nombre de mouvement case par case avant d'atteindre une forme concentré.

On peut juger que nous avons simplement retirer la contrainte sur le nombre et la direction des d éplacement de pion pour simplifier le jeu.

C'est une des fonctions les plus importantes.

### *Chaine la plus longue :*

Il s'agit simplement de calculer la chaîne de pions la plus longue. C'est une fonction très importante puisqu'elle traduit le nombre de pièces restantes à relier. Mais elle ne tient pas compte de la fragilité de sa forme. Ou de la difficulté à ramener les pièces lointaines

### *Nombre moyen de voisins :*

A mi-chemin entre la concentration et la chaîne la plus longue, elle traduit le nombre de liaisons entre les pions. Plus elles sont nombreuses, plus il est difficile de casser des chaînes et plus on s'approche d 'un dénouement.
C'est une fonction mineure mais elle renforce la concentration.

### *Nombre de pion :*

Le nombre de pion influence la difficulté à tous les rassembler, il serait mal avisé d'en capturer un trop grand nombre. On compte ici la différence de nombre entre les pions amis et ennemi.

Cette fonction a suscité un dilemme puisqu'elle grève les performances néanmoins elle permet d'obtenir la victoire face à celle qui n'en dispose pas. Elle a donc été maintenu dans l'heuristique.

## IMPLEMENTATION

### *Algorithmes de recherche dans un arbre*

Le choix de l'algorithme d'exploration s'est porté sur **Nega-SCOUT**.

Le comportement asymptotique de Nega-Scout est identique à αβ. Et selon les jeux il peut lui être supérieur, même si Nega-Scout semble mieux adapté aux arbres profonds, avec facteur de branchement faible, il domine αβ : il n'explore jamais un noeud qui serait coupé par αβ.

Et il se révèle particulièrement efficace lorsque le nombre de pion se réduit vers la fin de partie, lorsque le match se joue.

Le choix s'est porté sur lui car il est utilisé dans la plupart des IA d'échec, qui possèdent un facteur de branchements voisins du LOA.
Aux échecs, on constate un gain de 10% par rapport à neg-αβ voir 40% pour le jeu d'awélé, sans mesure il est probable tout de même que pour le LOA le gain soit compris entre ces deux cas.

Nous avons comparé les algorithmes « minimax », neg-αβ et nega-scout, les différences entre chacun sont très sensible de l'ordre du simple au double.

Tandis que nous avons fixé la profondeur par défaut à 3. Ce chiffre constitue le seuil maximal avant de dépasser la durée moyenne d'un coup au vu de la durée de la plupart des parties.
Toutefois si le temps de calcul est particulièrement court vu les performances matériel, la profondeur augmentera.
On considère qu'un coup doit être joué en une quinzaine de secondes et que nous mettons en moyenne vingt-cinqs coup à finir une partie.

### *Représentation*

```
  . x x x x x x .
  o . . . . . . o
  o . . . . . . o
  o . . . . . . o
  o . . . . . . o
  o . . . . . . o
  o . . . . . . o
  . x x x x x x .
```

Le plateau est souvent très souvent lu et utilisé, nous avons donc choisi de le remplir avec des Bytes afin de rendre l'ensemble du programme moins gourmand en espace.
Les pions ne contienne qu'un couple de coordonnée et leur couleur.

Nous ne stockons ni leur voisins immédiats, ni leurs nombres de mouvement.
Le choix du tableau nous permet de faire des économies au niveau de l'espace mais il reste cependant relativement lourd de parcourir l'ensemble du tableau (ce que nous faisons de nombreuses fois pour l'evaluation de celui-ci ou encore pour vérifier si une partie est finie).

L'accès direct aux données facilites aussi l'implémentation de certaines de nos heuristisques. Nous n'avons pas trouvés de structure de données qui conviendrait mieux à nos besoins pour ce projet. Nous avons a un momment envisagés une double listes chainés (voir même triple si on veut accéder facilement aux diagonales), mais nous n'avons pas poursuivis dans cette voix, car les moyens d'accès paraissaient trop contraignant (surtout pour toutes les opérations sur les diagonales).

### *Hasard*

Il a été laissé une chance de 5% à la machine de jouer un coup aléatoire sans faire de recherche, afin d'éviter un comportement trop automatique ou bien une « boucle » avec une autre IA.

Ajouter une partie d'aléatoire dans le comportement de notre IA peut permettre de mettre en échec certains algorithme adverse qui se basent sur le fait que le joueur adverse joue « bien ».

### *Conclusion*

Contre les pré-versions plus primitives de l'intelligence artificielle, le programme gagne systématiquement. Et la plupart des comportements illogique et inopportuns ont été éliminé.

Néanmoins, elle n'est pas capable de battre à coup sûr un joueur débutant, ne disposant pas de stratégies évoluées.

Elle est incapable de piéger une pièce adverse dans un coin pour l'empêcher de gagner par exemple. Et elle effectue encore de nombreux coups inutiles, même si elle se dirige globalement vers la victoire.

Une partie de cela est dut au relatif manque d'ajustement du programme, en effet, les sous-fonctions n'ont pas toute la même importance et en affectant des coefficients aux sous-fonctions puis en faisant s'affronter les IA dans un but de sélection naturelle. Nous obtiendrions un bon ajustement et des informations sur la qualités de chacunes des fonctions.
