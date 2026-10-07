# Demo: Navigation-Popup (Navigate to Declaration) + Hover Sync

Sandbox-IDE: `start-demo.bat` / Gradle `runIde`.

## Wenn überall „Cannot find declaration to go to“ erscheint

Das passiert, wenn die IDE **kein Navigationsziel** für die Stelle unter dem Caret findet (Dumb Mode, falsche Datei, reine Typen, fehlende Source Roots). **Nicht** ein Familia-Fehler — ohne Popup-Liste gibt es nichts zum Hovern.

**Zuverlässig getestet (Gradle):** `NavigationHoverDemoGotoDeclarationTest` + `ComponentSubtabNavigationTargetPopupHoverIntegrationTest`.

### Empfohlen — Java, eine Datei

1. `sidetabs-examples/java/NavigationHoverDemoStandalone.java` öffnen  
2. In `runNavigationHoverDemo()` den Namen **`familiaNavigationDemoTarget`** markieren (nicht nur Klammern)  
3. **Navigate to Declaration** (Ctrl+B) → Überladungs-Liste **in derselben Datei**  
4. Einträge hovern (Hover Sync) · Rechtsklick → **Im Projektbaum anzeigen**

Modul `demo-project` hat `sidetabs-examples/java` als Source Root (`.idea/demo-project.iml`).

### TypeScript — eine Datei (wenn TS-Analyse läuft)

1. `src/app/navigation-hover-demo/navigation-hover-demo.standalone.ts` öffnen  
2. Caret auf **`familiaNavigationDemoTarget`** in `runNavigationHoverDemo()`  
3. Ctrl+B → Popup → hovern wie oben  

Warte nach Projektstart, bis kein Dumb-Mode mehr aktiv ist. Ohne funktionierendes TypeScript-Plugin (Ultimate + indexiertes Demo) schlagen die **Angular-Komponenten** unten oft fehl — dann Java-Standalone nutzen.

## Einstellungen

`Settings | Tools | Familia`:

1. **Familia** — an  
2. **Hover Sync aktivieren** — an  
3. Rechtsklick **Im Projektbaum anzeigen** — mit Familia an

## Optional — mehrere Dateien (nur wenn Ctrl+B schon funktioniert)

| Szenario | Datei | Hinweis |
|----------|--------|---------|
| Angular + Anchor | `navigation-hover-demo.component.ts` + `navigation-hover-demo.anchor.ts` | Import über Dateigrenze; braucht TS-Index |
| User-Modelle | `CentralUser` / `FeatureUser` in der Komponente | `import type` → manchmal schwächer als Werte |
| Java zwei Dateien | `NavigationHoverDemoUsage.java` + `NavigationHoverDemoAnchor.java` | braucht Java-Modul/SDK |
| Dual loaders | `navigation-hover-demo.dual-loaders.ts` | zwei `user.model.ts`-Gruppen |

## Dateien

| Datei | Zweck |
|-------|--------|
| `sidetabs-examples/java/NavigationHoverDemoStandalone.java` | **Hauptdemo** (Java, getestet) |
| `navigation-hover-demo.standalone.ts` | **Hauptdemo** (TS, eine Datei) |
| `navigation-hover-demo.anchor.ts` | Überladungen für Komponenten-Demo |
| `navigation-hover-demo.component.*` | Subtab-Gruppe HTML/TS/SCSS |
| `NavigationHoverDemoAnchor.java` / `NavigationHoverDemoUsage.java` | Java Mehrdatei-Variante |
