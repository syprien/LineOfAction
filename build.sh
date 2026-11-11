#!/usr/bin/env bash
# Construit les jars du tournoi.
#   ./build.sh               version courante : build/tournoi/TazzzzSyprien.jar, Arbitre.jar, Rival.jar
#   ./build.sh v2008-rendu   une version de l'historique : build/tournoi/TazzzzSyprien_v2008-rendu.jar
# Nécessite javac et jar (Java 11 ou plus) dans le PATH.
set -euo pipefail
cd "$(dirname "$0")"

SRC=.
SUFFIXE=""
if [ $# -ge 1 ]; then
  SRC="build/$1"
  SUFFIXE="_$1"
  rm -rf "$SRC"
  mkdir -p "$SRC"
  git archive "$1" | tar -x -C "$SRC"
fi

CLASSES="build/classes$SUFFIXE"
rm -rf "$CLASSES"
mkdir -p "$CLASSES" build/tournoi
SOURCES=()
for p in net moteur ia jeux devoirLOA arbitre rival ultime; do
  [ -d "$SRC/$p" ] && while IFS= read -r f; do SOURCES+=("$f"); done < <(find "$SRC/$p" -name '*.java')
done
javac -nowarn --release 11 -encoding UTF-8 -d "$CLASSES" "${SOURCES[@]}"

fabriquer_jar() {
  local nom="$1" principale="$2"
  [ -f "$CLASSES/${principale//.//}.class" ] || return 0
  printf 'Main-Class: %s\n' "$principale" > build/manifest.txt
  jar cfm "build/tournoi/$nom.jar" build/manifest.txt -C "$CLASSES" .
  echo "build/tournoi/$nom.jar"
}

fabriquer_jar "TazzzzSyprien$SUFFIXE" devoirLOA.ClientJoueurReseauIALOA
if [ -z "$SUFFIXE" ]; then
  fabriquer_jar Arbitre arbitre.Arbitre
  fabriquer_jar Rival rival.Rival
  fabriquer_jar Ultime ultime.devoirLOA.ClientJoueurReseauIALOA
fi
