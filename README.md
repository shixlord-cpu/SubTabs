# SubTabs

Ein MVP für IntelliJ-basierte IDEs, der zusammengehörige Dateien einer
Komponente als schmale Subtabs direkt oberhalb des Editors anzeigt.

## Unterstützte Dateien

Die Leiste wird nur angezeigt, wenn mindestens zwei zusammengehörige Dateien
gefunden werden. Pro Kategorie wird ein Tab gezeigt. Beim Anklicken öffnet
IntelliJ die gewählte Datei im normalen Editor. Die meisten Gruppen suchen nur
im selben Ordner; State-Management sucht zusätzlich in benachbarten Ordnern.

### Angular-Komponenten

Dateien mit demselben Basisnamen:

- `*.ts`
- `*.spec.ts` oder `*.test.ts`
- `*.html`
- `*.scss`, `*.sass`, `*.css` oder `*.less`

### NPM-Konfiguration

Feste Dateinamen im selben Ordner:

- `package.json`
- `package-lock.json` oder `npm-shrinkwrap.json`
- `yarn.lock`, `pnpm-lock.yaml`, `bun.lock` / `bun.lockb`
- `.npmrc`
- `.nvmrc` oder `.node-version`

### TypeScript-Konfiguration

- `tsconfig.json`
- `tsconfig.base.json`
- `tsconfig.app.json`
- `tsconfig.spec.json`
- `tsconfig.lib.json`
- `tsconfig.editor.json`
- `tsconfig.build.json`

### Env-Dateien

- `.env`
- `.env.local`
- `.env.example` oder `.env.sample`
- `.env.development`
- `.env.production`
- `.env.test`

### State-Management

Dateien desselben Entity-Namens (`cart.actions.ts`, `cart.reducer.ts`, …):

- `*.actions.ts`
- `*.reducer.ts` / `*.reducers.ts`
- `*.effects.ts`
- `*.selectors.ts`
- `*.state.ts`
- `*.store.ts`
- `*.facade.ts`

Zwei Layouts werden erkannt:

- **Zentral:** alle Store-Dateien liegen im selben Ordner.
- **Feature-basiert:** zugehörige Store-Dateien liegen im selben Ordner **oder** in einem benachbarten Ordner (Geschwisterordner wie `products/` und `products-state/`).

### Models

Model-Daten desselben Entity-Namens im **selben Ordner**:

- `*.model.ts`
- `*.dto.ts`
- `*.entity.ts`
- `*.interface.ts`
- `*.type.ts`
- `*.mapper.ts`
- `*.mock.ts`

## Voraussetzungen

- IntelliJ IDEA 2025.3 oder neuer
- Internetzugang beim ersten Build
- JDK 21 bis 26 zum Ausführen von Gradle

Der Gradle Wrapper ist Teil des Projekts; eine globale Gradle-Installation ist
nicht erforderlich.

## MVP in einer Sandbox testen

Wichtig: `gradlew.bat` allein startet **keine IDE**. Es zeigt nur die Gradle-Hilfe.
Zum Testen brauchst du explizit `runIde`:

```powershell
.\gradlew.bat runIde
```

Alternativ auf Windows:

```powershell
.\start-demo.bat
```

Beim **ersten Start** kann nach `100% CONFIGURING` lange nichts Sichtbares passieren.
Gradle lädt dann oft im Hintergrund IntelliJ IDEA herunter (ca. 1–3 GB). Das ist normal
und kann 5–20 Minuten dauern. `start-demo.bat` nutzt `--console=plain`, damit du danach
einzelne Tasks wie `> Task :prepareSandbox` siehst.

Optional vorher einmalig ausführen (beschleunigt den ersten Demo-Start):

```powershell
.\warmup.bat
```

### Wenn `start-demo.bat` „hängt“

1. **Einfach warten** – besonders beim allerersten Mal (Download).
2. Prüfen, ob unten im Terminal Gradle-Zeilen erscheinen (`> Task :...`).
3. Falls ein Fehler zur **gesperrten JAR** kommt: Test-IDE schließen oder
   `.\gradlew.bat cleanSandbox` ausführen, dann erneut starten.
4. Mit Internetverbindung starten – Gradle muss IntelliJ-Dateien laden.

Das oeffnet automatisch das Demo-Projekt unter `demo-project/`.

1. Dieses Verzeichnis in IntelliJ IDEA oeffnen (optional, fuer Entwicklung).
2. Warten, bis der Gradle-Import abgeschlossen ist.
3. Die Gradle-Aufgabe `runIde` starten (siehe oben).
4. In der gestarteten Test-IDE sollte das Demo-Projekt bereits geoeffnet sein.
5. Oeffne `demo-project/src/app/user-card.component.ts`.
6. Direkt unter dem normalen Editor-Tab erscheinen die Subtabs `TS`, `Test`, `HTML` und `SCSS`.
7. Jeden Subtab anklicken und pruefen, ob die passende Datei geoeffnet wird.
8. Oeffne `demo-project/package.json`. Es erscheinen Subtabs wie `Package`, `Lock`, `npmrc` und `nvm`.
9. Oeffne `tsconfig.json` und `.env` im Projektroot.
10. Oeffne `src/app/state-management/central/cart.actions.ts` (zentraler Store).
11. Oeffne `src/app/state-management/feature-based/products/products.actions.ts`
    (Feature-Store über `products/` und `products-state/`).
12. Oeffne `src/app/models/central/user.model.ts` und
    `src/app/models/feature-based/user/user.model.ts`.

## Automatisierte Tests und Build

```powershell
.\gradlew.bat test
.\gradlew.bat buildPlugin
```

Das installierbare ZIP liegt danach unter `build\distributions\`.

Zur Installation in der normalen IDE:

1. `Settings | Plugins` öffnen.
2. Über das Zahnrad `Install Plugin from Disk...` wählen.
3. Das ZIP aus `build\distributions\` auswählen.
4. Die IDE neu starten.

## Einstellungen

Unter `Settings | Tools | SubTabs`:

- **SubTabs anzeigen** (standardmäßig an). Entspricht dem ausgeklappten Zustand; deaktivieren blendet die Leiste aus und entspricht dem Einklappen über das Symbol.
- **Einklappen-Symbol anzeigen** (standardmäßig an). Blendet das Minimize-/Expand-Icon aus. SubTabs lassen sich dann nur noch über die Einstellung ein- und ausschalten.
- **Subtab-Regeln**: Tabelle mit allen mitgelieferten Regeln (npm, tsconfig, env, State, Model, Komponente) plus eigene Ergänzungen.
- **Beim Hover über einen Subtab zur Datei im Projektbaum scrollen** (standardmäßig aus).
  Ist die Option aus, wird die Datei nur gehovert, wenn sie im Projektbaum bereits sichtbar ist.

## Tastenkürzel

Wenn SubTabs sichtbar sind:

- **Alt+←** — vorheriger Subtab
- **Alt+→** — nächster Subtab

Anpassbar unter `Settings | Keymap` („Vorheriger Subtab“ / „Nächster Subtab“).

## Projektstruktur

- `ComponentSubtabsManager` / `ComponentSubtabBar`: erzeugt die scrollbare Leiste direkt über dem Editor.
- `ComponentEditorTabTitleProvider`: zeigt den Komponentennamen im Haupttab (z. B. `user-card` statt `user-card.component.ts`).
- `ComponentFileNaming`: erkennt Dateigruppen und definiert deren Reihenfolge.
- `plugin.xml`: registriert die Editor-Erweiterung.
- `ComponentFileNamingTest`: testet die Dateinamenerkennung.

## Bewusste Grenzen des MVP

- Verwandte Dateien werden meist im selben Ordner gesucht; State-Management
  berücksichtigt zusätzlich benachbarte Ordner.
- Es gibt noch keine Einstellungen für eigene Suffixe oder Tab-Reihenfolgen.
- Änderungen im Dateisystem werden beim erneuten Anzeigen des Editors sichtbar;
  eine sofortige Aktualisierung einer bereits sichtbaren Leiste folgt später.
- Der Code verwendet ausschließlich IntelliJ-Platform-APIs. Dadurch ist die
  spätere Anpassung für WebStorm klein; VS Code benötigt eine separate
  TypeScript-Implementierung derselben Dateigruppierungslogik.
