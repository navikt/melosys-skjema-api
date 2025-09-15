# Fullmaktmodell for Melosys Søknadsskjema

## Oversikt

Dette dokumentet beskriver de ulike fullmaktscenarioene i søknadssystemet for utsendte arbeidstakere. Systemet håndterer komplekse relasjoner mellom flere aktører som kan opptre på vegne av hverandre.

## Sentrale begreper og roller

### Primære roller
- **Arbeidsgiver**: Norsk virksomhet som sender ut arbeidstaker
- **Arbeidstaker**: Person som sendes ut for arbeid i EU/EØS-land
- **Rådgiverfirma**: Konsulentfirma som bistår arbeidsgivere
- **Fullmektig for arbeidstaker**: Person eller organisasjon som kan fylle inn på vegne av arbeidstaker

### Viktige distinksjoner
- **Altinn-delegering**: Gir tilgang til å opptre på vegne av en organisasjon (brukes mellom rådgiverfirma og arbeidsgiver)
- **Fullmakt for søknad**: Gir tilgang til å fylle inn arbeidstaker-delen av en spesifikk søknad
- **Representasjon**: Når man logger inn og velger å opptre som en organisasjon

---

## Fullmaktscenarioer

### Scenario 1: Rådgiverfirma → Arbeidsgiver → Arbeidstaker

```mermaid
sequenceDiagram
    participant RF as Beta Rådgivning AS (Rådgiverfirma)
    participant AG as Alfa Industri AS (Arbeidsgiver)
    participant AT as Arbeidstaker
    participant System as NAV System
    participant Altinn as Altinn
    
    Note over RF,Altinn: FORUTSETNING: Alfa Industri AS har delegert tilgang i Altinn
    
    RF->>System: Logger inn på nav.no
    System->>Altinn: Henter representasjoner
    Altinn-->>System: Returnerer Alfa Industri AS som valgbar rolle
    RF->>System: Velger å opptre som Alfa Industri AS
    
    Note over RF,System: Beta Rådgivning AS opptrer nå som Alfa Industri AS
    
    RF->>System: Starter søknad for Alfa Industri AS
    RF->>System: Fyller arbeidsgiver-del
    RF->>System: Ønsker å fylle for arbeidstaker
    
    System->>AT: Fullmaktforespørsel
    Note over System,AT: MÅ AVKLARES: Hvem får fullmakt?<br/>Alt 1: Kun Beta Rådgivning AS<br/>Alt 2: Både Beta Rådgivning AS og Alfa Industri AS<br/>Forespørsel må presisere hvem som får tilgang og mottar brev
    
    alt Arbeidstaker godkjenner
        AT->>System: Godkjenner fullmakt
        RF->>System: Fyller arbeidstaker-del
        RF->>System: Sender inn komplett søknad
    else Arbeidstaker avslår
        AT->>System: Avslår fullmakt
        System->>AT: Varsler om å fylle selv
        AT->>System: Fyller sin del selv
    end
```

**Viktige poenger:**
- **AVKLARING PÅKREVD**: Hvem får fullmakten - kun rådgiverfirma eller både rådgiverfirma og arbeidsgiver?
- Fullmaktforespørselen må tydelig presisere hvem som får tilgang og hvem som mottar brev
- Altinn-delegeringen gir kun tilgang til å opptre som arbeidsgiver, ikke automatisk fullmakt for arbeidstaker
- Fullmakten er knyttet til den spesifikke søknaden

---

### Scenario 2: Arbeidsgiver → Arbeidstaker (uten rådgiverfirma)

```mermaid
sequenceDiagram
    participant AG as Arbeidsgiver (Alfa Industri AS)
    participant AT as Arbeidstaker
    participant System as NAV System
    
    AG->>System: Logger inn direkte
    AG->>System: Starter søknad
    AG->>System: Fyller arbeidsgiver-del
    AG->>System: Ønsker å fylle for arbeidstaker
    
    System->>AT: Fullmaktforespørsel fra Alfa Industri AS
    
    alt Arbeidstaker godkjenner
        AT->>System: Godkjenner fullmakt
        Note over AG,AT: Fullmakten går til Alfa Industri AS
        AG->>System: Fyller arbeidstaker-del
        AG->>System: Sender inn komplett søknad
    else Arbeidstaker avslår
        AT->>System: Avslår fullmakt
        System->>AT: Varsler om å fylle selv
        AT->>System: Fyller sin del selv
    end
```

**Viktige poenger:**
- Fullmakten går direkte til arbeidsgiver (Alfa Industri AS)
- Hvis rådgiverfirma senere skulle få Altinn-delegering, kan de IKKE se arbeidstaker-delen for den søknaden
- Fullmakten er knyttet til den spesifikke søknaden

---

### Scenario 3: Arbeidstaker-initiert søknad

```mermaid
stateDiagram-v2
    [*] --> ArbeidstakerStarter: Arbeidstaker logger inn
    
    ArbeidstakerStarter --> FyllerEgenDel: Fyller sin del
    FyllerEgenDel --> OppgirArbeidsgiver: Oppgir organisasjonsnummer
    
    OppgirArbeidsgiver --> ValgFullmakt: Velger fullmaktstrategi
    
    ValgFullmakt --> GiFullmakt: Gi fullmakt til annen
    ValgFullmakt --> IngenFullmakt: Fyll selv
    
    GiFullmakt --> OppgirFullmektig: Oppgir person/organisasjon
    OppgirFullmektig --> FullmektigFyller: Fullmektig kan fylle
    
    IngenFullmakt --> VarslerArbeidsgiver: System varsler arbeidsgiver
    
    VarslerArbeidsgiver --> ArbeidsgiverFyller: Arbeidsgiver fyller sin del
    FullmektigFyller --> ArbeidsgiverFyller
    
    ArbeidsgiverFyller --> Matching: System matcher delene
    Matching --> SøknadKomplett: Begge deler mottatt
    SøknadKomplett --> Journalføring: Arbeidstaker-del utløser alltid journalføring
    
    Journalføring --> [*]
```

**To mulige implementasjoner (ikke avklart):**

#### Alternativ A: Arbeidstaker gir fullmakt proaktivt
- Arbeidstaker kan oppgi en fullmektig (person/organisasjon)
- Fullmektigen får varsel og kan fylle arbeidstaker-delen
- Eksempler på fullmektig: advokat, familiemedlem, annen tredjepart

#### Alternativ B: Fullmektig-initiert (mest sannsynlig)
- En person/organisasjon logger inn
- Starter arbeidstaker-del for annen person
- Sender fullmaktforespørsel til arbeidstaker
- Kan fylle inn hvis godkjent
- Fullmektig kan være: advokat, annen privatperson, tredjeparts organisasjon

---

### Scenario 4: Oversikt over mulige fullmaktrelasjoner

```mermaid
graph TD
    subgraph "Mulige fullmaktrelasjoner"
        RÅD[Rådgiverfirma]
        AG[Arbeidsgiver]
        AT[Arbeidstaker]
        FM[Fullmektig for arbeidstaker<br/>Advokat/Person/Organisasjon]
        
        RÅD -->|Altinn-delegering| AG
        RÅD -.->|Fullmakt for søknad| AT
        AG -.->|Fullmakt for søknad| AT
        FM -.->|Fullmakt for søknad| AT
    end
    
    style RÅD fill:#e1f5fe
    style AG fill:#fff3e0
    style AT fill:#f3e5f5
    style FM fill:#e8f5e9
```

**Forklaring:**
- Heltrukken linje = Altinn-delegering (organisatorisk tilgang)
- Stiplet linje = Fullmakt for spesifikk søknad
- Fullmektig for arbeidstaker kan være advokat, privatperson eller tredjeparts organisasjon

---

## Matching av søknadsdeler

Når arbeidstaker og arbeidsgiver fyller inn uavhengig av hverandre, må systemet matche delene:

```mermaid
flowchart LR
    subgraph "Arbeidstaker-del"
        AT_FNR[Personnummer: 12345678901]
        AT_ORG[Oppgitt org: 999888777]
    end
    
    subgraph "Arbeidsgiver-del"
        AG_FNR[Oppgitt ansatt: 12345678901]
        AG_ORG[Organisasjonsnr: 999888777]
    end
    
    AT_FNR -.->|Match| AG_FNR
    AT_ORG -.->|Match| AG_ORG
    
    AT_FNR --> KOMPLETT[Komplett søknad]
    AG_FNR --> KOMPLETT
```

**Matchingskriterier:**
- Personnummer (arbeidstaker) må matche oppgitt ansatt (arbeidsgiver)
- Organisasjonsnummer må matche på begge sider
- Begge deler må være sendt inn for komplett søknad

**Viktig om journalføring:**
- Journalføring starter når arbeidstaker sender inn sin del (uavhengig av arbeidsgiver-status)
- Søknaden gjelder alltid arbeidstakeren juridisk sett
- Oversiktssiden viser alltid status for begge deler

---

## Tilgangskontroll-matrise

| Aktør | Rolle | Kan se/redigere | Forutsetning |
|-------|-------|-----------------|--------------|
| Rådgiverfirma | Opptrer som arbeidsgiver | Arbeidsgiver-del | Altinn-delegering |
| Rådgiverfirma | Opptrer som arbeidsgiver | Arbeidstaker-del | Fullmakt fra arbeidstaker til RÅDGIVERFIRMA |
| Arbeidsgiver | Seg selv | Arbeidsgiver-del | Alltid |
| Arbeidsgiver | Seg selv | Arbeidstaker-del | Fullmakt fra arbeidstaker til ARBEIDSGIVER |
| Arbeidstaker | Seg selv | Arbeidstaker-del | Alltid |
| Arbeidstaker | Seg selv | Arbeidsgiver-del | Aldri |
| Fullmektig | For arbeidstaker | Arbeidstaker-del | Fullmakt fra arbeidstaker |

---

## Viktige prinsipper

### 1. Fullmakt følger initiativtaker (MÅ AVKLARES)
- Hvis rådgiverfirma (via arbeidsgiver-rolle) ber om fullmakt → fullmakt til rådgiverfirma (eller begge?)
- Hvis arbeidsgiver (direkte) ber om fullmakt → fullmakt til arbeidsgiver
- Fullmakten er IKKE transitiv gjennom Altinn-delegering

### 2. Uavhengighet
- Arbeidsgiver og arbeidstaker kan sende inn sine deler uavhengig
- Systemet matcher automatisk basert på personnummer og organisasjonsnummer
- Ingen part må vente på den andre for å sende sin del

### 3. Søknadsspesifikk fullmakt
- Fullmakt gjelder for én spesifikk søknad (bekreftet beslutning)
- Ikke generell fullmakt for alle fremtidige søknader
- Lettere å implementere og sikrere for brukeren
- Gjelder for ALLE fullmaktscenarioer

### 4. Synlighet
- Altinn-delegering gir IKKE automatisk tilgang til arbeidstaker-delen
- Hver fullmakt må eksplisitt godkjennes av arbeidstaker
- **MÅ AVKLARES**: Kan rådgiverfirma som har fått fullmakt se arbeidstaker-delen når arbeidsgiver ikke kan?

---

## Terminologi-ordbok

For å unngå misforståelser, bruk disse begrepene konsekvent:

| Term | Definisjon | Eksempel |
|------|------------|----------|
| **Altinn-delegering** | Organisatorisk tilgang via Altinn | Alfa Industri AS gir Beta Rådgivning AS tilgang |
| **Fullmakt for søknad** | Tillatelse til å fylle arbeidstaker-del | Arbeidstaker gir fullmakt til Beta Rådgivning AS |
| **Representasjon** | Å opptre på vegne av organisasjon | Beta Rådgivning-ansatt velger Alfa Industri AS-rolle |
| **Fullmektig** | Den som har fått fullmakt | Beta Rådgivning AS er fullmektig for arbeidstaker |
| **Fullmaktsgiver** | Den som gir fullmakt | Arbeidstaker er fullmaktsgiver |
| **Initiativtaker** | Den som ber om fullmakt | Beta Rådgivning AS eller Alfa Industri AS |
| **Matching** | Automatisk sammenkobling av søknadsdeler | System matcher via FNR + orgnr |

---

## Åpne spørsmål og avklaringsbehov

### Må avklares
1. **NAVs eksisterende fullmaktsløsning**: NAV har allerede en fullmaktsløsning for person-til-person representasjon. Skal vi:
   - Bruke NAVs eksisterende løsning for person-til-person fullmakter?
   - Bygge vår egen løsning for både person og organisasjon?
   - *Merk: NAVs løsning støtter IKKE organisasjoner, kun personer*
2. **Hvem får fullmakt i scenario 1**: Når rådgiverfirma ber om fullmakt - får kun de fullmakt, eller både rådgiverfirma og arbeidsgiver?
3. **Synlighet for rådgiverfirma**: Kan rådgiverfirma med fullmakt se arbeidstaker-delen selv om arbeidsgiver ikke kan?
4. **Arbeidstaker-initiert fullmakt**: Skal vi gå for alternativ A eller B? (Se scenario 3)
5. **Tilbaketrekking**: Kan arbeidstaker trekke tilbake fullmakt etter den er gitt?
6. **Historikk**: Skal fullmektig se historiske søknader?
7. **Brev og kommunikasjon**: Hvem mottar brev når fullmakt er gitt?

### Tekniske beslutninger
1. **Datamodell**: Fullmakt per søknad ✅ (BESLUTTET)
2. **Timeout**: 30 dager (FORESLÅTT - må bekreftes)
3. **Varsling**: ✅ Implementeres allerede:
   - Personer får varsel på nav.no (Min side)
   - Organisasjoner får varsel på Altinn
   - Arbeidstaker får oppgave ved fullmaktforespørsel
   - Arbeidstaker får melding når søknad er sendt inn
4. **Implementeringsstrategi for fullmakt**: 🟡 UNDER AVKLARING
   - Alternativ A: Integrere med NAVs eksisterende fullmaktsløsning (kun for person-til-person)
   - Alternativ B: Bygge egen fullmaktsløsning (støtter både person og organisasjon)
   - *Vurdering: Egen løsning gir mer fleksibilitet og kan utvides til organisasjoner*

---

## Kommunikasjonstips

For å unngå forvirring i diskusjoner:

1. **Vær eksplisitt om hvem som får fullmakten**
   - ❌ "De får fullmakt"
   - ✅ "Beta Rådgivning AS får fullmakt fra arbeidstaker"

2. **Skill mellom Altinn-delegering og søknadsfullmakt**
   - ❌ "Rådgiverfirma har fullmakt"
   - ✅ "Beta Rådgivning AS har Altinn-delegering fra Alfa Industri AS OG fullmakt fra arbeidstaker"

3. **Bruk konkrete eksempler med navn**
   - ❌ "Rådgiverfirma sender for arbeidsgiver"
   - ✅ "Beta Rådgivning AS sender søknad på vegne av Alfa Industri AS"

4. **Vær tydelig på kontekst**
   - ❌ "Han kan se søknaden"
   - ✅ "Beta Rådgivning-ansatt kan se arbeidstaker-delen fordi arbeidstaker ga fullmakt til Beta Rådgivning AS"

---

*Dette dokumentet er et levende dokument som oppdateres når flere detaljer avklares.*