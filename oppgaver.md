# Melosys A1-skjema - Epic, stories og oppgaver

## EPIC: Digitalt A1-skjema på Nav.no

**Beskrivelse:** Erstatte Altinn-skjema for utsendt arbeidstaker med moderne løsning på Nav.no

---

## Stories og oppgaver

### Story 1A: Backend prosjektoppsett

**Som:** Utviklingsteam  
**Ønsker jeg:** En fungerende backend-plattform  
**Slik at:** Vi kan utvikle API og integrasjoner

**Oppgaver:**
- **TASK-1A.1:** Sett opp Spring Boot med Kotlin, database og NAIS-konfigurasjon
- **TASK-1A.2:** Implementer basis REST-endepunkter og health checks
- **TASK-1A.3:** Sett opp Kafka-producer og meldingsformat

---

### Story 1B: Frontend prosjektoppsett

**Som:** Utviklingsteam  
**Ønsker jeg:** En fungerende frontend-plattform  
**Slik at:** Vi kan utvikle brukergrensesnitt

**Oppgaver:**
- **TASK-1B.1:** Sett opp React 18 med TypeScript, Node/Express proxy, Aksel, routing og basis layout

---

### Story 2: Innlogging og rollevalg

**Som:** Bruker  
**Ønsker jeg:** Å logge inn og velge hvem jeg representerer  
**Slik at:** Jeg kan fylle ut skjema for riktig part

**Oppgaver:**
- **TASK-2.1:** Implementer ID-porten token-utveksling med backend og frontend
- **TASK-2.2:** Opprett ressurs/delegering i Altinn for A1-skjema
- **TASK-2.3:** Integrer Altinn for å hente representasjoner
- **TASK-2.4:** Implementer rollevalg-UI og kontekstbytte

---

### Story 3: Arbeidsgiver-skjema

**Som:** Arbeidsgiver  
**Ønsker jeg:** Å fylle ut arbeidsgiverdelen av A1-skjemaet  
**Slik at:** Jeg kan søke om utsending for min ansatt

**Oppgaver:**
- **TASK-3.1:** Lag skjema-API med CRUD-operasjoner og validering
- **TASK-3.2:** Implementer arbeidsgiver-skjema UI med valg for arbeidstaker-utfylling
- **TASK-3.3:** Implementer preutfylling fra Enhetsregisteret og A-reg

---

### Story 4: Arbeidstaker-skjema og varsling

**Som:** Arbeidstaker  
**Ønsker jeg:** Å bli varslet og kunne fylle ut min del  
**Slik at:** Søknaden blir komplett

**Oppgaver:**
- **TASK-4.1:** Integrer Nav-melding for varsling
- **TASK-4.2:** Lag arbeidstaker-skjema UI med validering
- **TASK-4.3:** Implementer preutfylling fra PDL
- **TASK-4.4:** Håndter uavhengig innsending av skjemadeler

---

### Story 5: Fullmaktsystem

**Som:** Arbeidsgiver  
**Ønsker jeg:** Å kunne be om fullmakt fra arbeidstaker  
**Slik at:** Jeg kan fylle ut hele skjemaet på deres vegne

**Oppgaver:**
- **TASK-5.1:** Implementer fullmakt-API med forespørsel og beslutning
- **TASK-5.2:** Lag fullmakt-UI og håndter tilgangskontroll basert på fullmaktstatus

---

### Story 6: Skjemaoversikt og status

**Som:** Bruker  
**Ønsker jeg:** Å se oversikt over mine skjemaer  
**Slik at:** Jeg har kontroll på status og fremdrift

**Oppgaver:**
- **TASK-6.1:** Lag API for å liste skjemaer
- **TASK-6.2:** Implementer dashboard med skjemaliste og statusvisning
- **TASK-6.3:** Lag detaljvisning for enkeltskjemaer

---

### Story 7: Innsending og arkivering

**Som:** System  
**Ønsker jeg:** At komplette skjemaer sendes til saksbehandling  
**Slik at:** Søknader behandles i Melosys

**Oppgaver:**
- **TASK-7.1:** Publiser melding om innsendt søknad på Kafka
- **TASK-7.2:** Implementer REST-endepunkt for Melosys-API
- **TASK-7.3:** Generer og lagre PDF av innsendt skjema

---

### Story 8: Overvåking

**Som:** Driftsteam  
**Ønsker jeg:** Å overvåke systemet  
**Slik at:** Vi kan oppdage og løse problemer raskt

**Oppgaver:**
- **TASK-8.1:** Sett opp Grafana dashboards og alerts

---
