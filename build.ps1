# Construit les jars du tournoi.
#   ./build.ps1               version courante : build/tournoi/TazzzzSyprien.jar, Arbitre.jar, Rival.jar
#   ./build.ps1 v2008-rendu   une version de l'historique : build/tournoi/TazzzzSyprien_v2008-rendu.jar
# Nécessite javac et jar (Java 11 ou plus) dans le PATH.
param([string]$Etiquette = "")
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$src = "."
$suffixe = ""
if ($Etiquette) {
    $src = "build/$Etiquette"
    $suffixe = "_$Etiquette"
    if (Test-Path $src) { Remove-Item -Recurse -Force $src }
    New-Item -ItemType Directory -Force $src | Out-Null
    git archive --format=zip -o "build/$Etiquette.zip" $Etiquette
    if ($LASTEXITCODE) { throw "git archive a échoué" }
    Expand-Archive "build/$Etiquette.zip" -DestinationPath $src
    Remove-Item "build/$Etiquette.zip"
}

$classes = "build/classes$suffixe"
if (Test-Path $classes) { Remove-Item -Recurse -Force $classes }
New-Item -ItemType Directory -Force $classes, "build/tournoi" | Out-Null
$sources = foreach ($p in "net", "moteur", "ia", "jeux", "devoirLOA", "arbitre", "rival") {
    if (Test-Path "$src/$p") { Get-ChildItem -Recurse -Filter *.java "$src/$p" | ForEach-Object FullName }
}
javac -nowarn --release 11 -encoding UTF-8 -d $classes @sources
if ($LASTEXITCODE) { throw "javac a échoué" }

function Fabriquer-Jar($nom, $principale) {
    if (-not (Test-Path "$classes/$($principale -replace '\.', '/').class")) { return }
    Set-Content -Encoding ascii build/manifest.txt "Main-Class: $principale"
    jar cfm "build/tournoi/$nom.jar" build/manifest.txt -C $classes .
    if ($LASTEXITCODE) { throw "jar a échoué" }
    "build/tournoi/$nom.jar"
}

Fabriquer-Jar "TazzzzSyprien$suffixe" devoirLOA.ClientJoueurReseauIALOA
if (-not $suffixe) {
    Fabriquer-Jar Arbitre arbitre.Arbitre
    Fabriquer-Jar Rival rival.Rival
}
