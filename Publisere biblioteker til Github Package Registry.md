---
layout: default
title: Publisere til GPR
parent: Teknisk
nav_order: 5
---

# Publisere biblioteker til Github Package Registry

Å laste opp pakker til Github Package Registry er et enklere alternativ
for å publisere Maven-artifakter enn å bruke Maven Central.

Den enkleste måten å publisere artifakter dit, er å sette opp en CI-pipeline
via Github Actions.

NB! Denne oppskriften setter opp en release-prosess der *hver commit til master blir releaset*.
Hvis det, av en eller annen grunn, ikke er ønskelig, så må det gjøres noen tilpasninger. Disse
er ikke dokumentert her (ennå).

# Steg 1: Sjekk at pom.xml er riktig

Det er visse krav til pom.xml-fila:


```
<groupId>no.nav</groupId>
<artifactId>NAVN PÅ PAKKEN DIN</artifactId>
<licenses>
    <license>
        <name>MIT License</name>
        <url>https://opensource.org/licenses/MIT</url>
    </license>
</licenses>
<developers>
    <developer>
        <organization>Nav (Arbeids- og velferdsdirektoratet) - The Norwegian Labour and Welfare Administration</organization>
        <organizationUrl>https://www.nav.no</organizationUrl>
    </developer>
</developers>
<distributionManagement>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/navikt/NAVN-PÅ-GITHUB-REPOET-DITT</url>
    </repository>
</distributionManagement>
<scm>
    <developerConnection>scm:git:git@github.com:navikt/NAVN-PÅ-GITHUB-REPOET-DITT.git</developerConnection>
    <connection>scm:git:git@github.com:navikt/NAVN-PÅ-GITHUB-REPOET-DITT.git</connection>
    <url>https://github.com/navikt/NAVN-PÅ-GITHUB-REPOET-DITT</url>
    <tag>HEAD</tag>
</scm>
<build>
    <plugins>
        <plugin>
            <artifactId>maven-source-plugin</artifactId>
            <executions>
                <execution>
                    <id>include-sources</id>
                    <goals>
                        <goal>jar-no-fork</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

En forskjell sammenlighet med publisering til Maven Central, er at Maven Central har strengere krav - blant
annet til at man bruker javadoc-plugin (til å generere javadoccs) og gpg-plugin (til å generere
signaturer for artifaktene). Dette er ikke krav i Github Package Registry.

# Steg 2: Opprett en fil `maven-settings.xml` i rotkatalogen til prosjektet

```
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>x-access-token</username>
            <password>${env.GITHUB_TOKEN}</password>
        </server>
    </servers>
</settings>
```

Denne fila konfigurerer credentials for å publisere til Github Package Registry.

# Steg 3: Lag et av release-scriptene `release.sh` i rotkatalogen
## Legg inn versjonering med timestamped commit sha
```
#!/bin/bash
set -e

TIME=$(TZ="Europe/Oslo" date +%Y.%m.%d-%H.%M)
COMMIT=$(git rev-parse --short=12 HEAD)
VERSION="1.$TIME-$COMMIT"
echo "Setting version $VERSION"

mvn -B versions:set -DnewVersion="$VERSION"
mvn -B versions:commit

echo "Running release"
mvn -B --settings maven-settings.xml deploy -Dmaven.wagon.http.pool=false
```
## Legg inn versjonering med semantic versioning
Skriptet nedenfor vil lage et semantisk versjonert nummer som vil bli økt med 1 for hvert release
(i tillegg til commit-hash), på formatet `<major version>.<minor version>.<patch version>-<commit sha>`  
```
#!/bin/bash
set -e

mvn -B help:evaluate -Dexpression=project.version | tee project_version

if [ $? -gt 0 ]
 then
   echo "something fishy happened"
   exit 1;
fi

SEMANTIC_VERSION_WITH_SHA=$(cat project_version | grep -v INFO | grep -v WARNING)
SEMANTIC_VERSION_INCLUDING_PATCH=${SEMANTIC_VERSION_WITH_SHA%-*}
SEMANTIC_VERSION_EXCLUDING_PATCH=${SEMANTIC_VERSION_INCLUDING_PATCH%.*}
PATCH_VERSION=$(echo "$SEMANTIC_VERSION_INCLUDING_PATCH" | sed "s/$SEMANTIC_VERSION_EXCLUDING_PATCH.//")
NEW_PATCH_VERSION=$(($PATCH_VERSION+1))
COMMIT_SHA=$(git rev-parse --short=12 HEAD)
VERSION="$SEMANTIC_VERSION_EXCLUDING_PATCH.$NEW_PATCH_VERSION-$COMMIT_SHA"
echo "Setting version $VERSION"

mvn -B versions:set -DnewVersion="$VERSION"
mvn -B versions:commit

echo "Running release"
mvn -B --settings maven-settings.xml deploy -Dmaven.wagon.http.pool=false
```
## Gjør skriptet kjørbart

Husk `git update-index --chmod=+x release.sh` for å gjøre scriptet kjørbart, før det commites til git.

# Steg 4: Lag en Github Action for bygg

Opprett mappa `.github/workflows` i rotkatalogen til prosjektet.

I denne mappa kan du legge inn `build.yml`:
```
name: CI

on:
  push:
    branches:
      - '!master'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v1
    - name: Set up JDK 1.8
      uses: actions/setup-java@v1
      with:
        java-version: 1.8
    - name: Build with Maven
      run: mvn clean install
```

Denne jobben vil kjøre på alle brancher bortsett fra master-branchen, og kan være
nyttig for å verifisere at tester i pull-requester er grønne, før man merger inn til master-branchen.

Legg også inn `release.yml`:

```
name: Release (Github Package Registry)

on:
  push:
    branches:
    - 'master'

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v1
      - name: Set up JDK 1.8
        uses: actions/setup-java@v1
        with:
          java-version: 1.8
      - name: Run Maven release
        run: ./release.sh
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

Denne jobben kjører kun på `master`-branchen. For hver commit (eller merge) til
branchen, vil release-jobben kjøre, og trigge scriptet `release.sh`.

# Steg 5: Profitt

Commit noe til `master`-branchen, og sjekk at Github-actionen kjører som den skal.
I så fall skal biblioteket vises i grensesnittet til Github:

`https://github.com/navikt/NavN-PÅ-GITHUB-REPOET-DITT/packages`
