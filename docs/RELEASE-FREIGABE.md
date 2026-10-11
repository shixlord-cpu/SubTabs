# TabZ — Schritt-für-Schritt Freigabe (Closed Test)

Arbeite die Schritte **der Reihe nach** ab. Hake ab, wenn erledigt.

**Stand Projekt:** Version `0.1.0`, Plugin-ID `com.zayax.tabz`, ZIP-Name `tabz-0.1.0.zip` (Gradle-Projektname)

---

## Schritt 1 — JetBrains-Konto & Vendor-Profil

1. Einloggen (Browser, gleiches Konto wie IDE): [Account](https://account.jetbrains.com/) → danach [plugins.jetbrains.com](https://plugins.jetbrains.com/)

2. **Vendor anlegen** (einer der Wege — `/vendor/edit` gibt oft **404**, bis ein Vendor existiert):

   - **Neu:** [Create vendor](https://plugins.jetbrains.com/vendor/new)  
   - **Oder:** Profil → Abschnitt **Vendors** → **Create new Vendor**  
     ([Author profile](https://plugins.jetbrains.com/author/me) — eingeloggt)
   - **Oder:** Vendor beim ersten Plugin-Upload wählen/anlegen: [Upload plugin](https://plugins.jetbrains.com/plugin/add)

3. Ausfüllen:
   - **Vendor ID** ( später **nicht** änderbar — z. B. `ZaYaX`)
   - **Public name**, **E-Mail**, optional Website
   - **Trader / non-trader** (EU-Angabe, Pflicht)

4. **Bestehenden Vendor bearbeiten:** erst auf [Author profile](https://plugins.jetbrains.com/author/me) deinen Vendor anklicken → dann Edit / Einstellungen (nicht blind `/vendor/edit` ohne Vendor).

**Erledigt?** → Weiter zu Schritt 2.

---

## Schritt 2 — Plugin Signing (Pflicht für Marketplace)

Marketplace verlangt ein **signiertes** ZIP. Zertifikat einmalig erzeugen:

1. Anleitung: [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)
2. Empfohlen: Zertifikat über JetBrains / Marketplace-Workflow erzeugen (Abschnitt *Generate a certificate* in der Doku).
3. Du erhältst bzw. erzeugst:
   - **Private Key** (`.pem`)
   - **Certificate chain** (`.crt` / PEM)
   - **Passwort** für den Key

Bewahre die Dateien **lokal** auf (Ordner `certificate/` ist in `.gitignore`).

**Erledigt?** → Schritt 3.

---

## Schritt 3 — Publish-Token

1. Einloggen auf [plugins.jetbrains.com](https://plugins.jetbrains.com/)
2. **Token-Seite:** [My Tokens / Author tokens](https://plugins.jetbrains.com/author/me/tokens)  
   (Alternativ: Profil/Account → Bereich **Tokens** / **My Tokens**)
3. **Generate New Token** (permanenter Token)
   - **Scope:** **Marketplace** (Upload/Releases)
   - Name z. B. `TabZ-publish`
4. Token **sofort kopieren** — beginnt oft mit `perm:…`  
   **Wichtig:** Ein gespeicherter Token kann **nicht wieder angezeigt** werden. Verloren → neuen Token erzeugen, alten ggf. widerrufen.

Zwei Wege (reicht **einer** für Gradle — am besten **beide** nutzen):

| Wo | Wofür |
|----|--------|
| **`closed-test.env`** | `upload-closed-test.bat` lädt das (nur Upload, nicht Build) |
| **Windows-Benutzer-Umgebung** | `gradlew publishPlugin` aus IDE/neuem Terminal ohne BAT |

**Erledigt?** → Schritt 4.

---

## Schritt 4 — Secrets lokal

### A) `closed-test.env` (Pflicht für BAT)

1. `closed-test.env.example` → **`closed-test.env`** (bei dir: Token steht schon drin).
2. Später Signing: `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`, `CERTIFICATE_CHAIN` ergänzen.

Datei **nicht** committen (`.gitignore`).

### B) Windows-Umgebungsvariable (optional, aber sinnvoll)

Einmal ausführen (liest **`closed-test.env`**, setzt **Benutzer**-Variablen):

```powershell
.\setup-windows-env-from-closed-test.bat
```

Danach **neues Terminal** oder **IDE neu starten** — sonst sieht die laufende Session die Werte nicht.

Prüfen (neues PowerShell-Fenster):

```powershell
[Environment]::GetEnvironmentVariable('PUBLISH_TOKEN','User')
```

Manuell geht auch: **Windows-Einstellungen → System → Info → Erweiterte Systemeinstellungen → Umgebungsvariablen → Benutzervariablen → Neu** → Name `PUBLISH_TOKEN`, Wert `perm:…`.

Optional:

```powershell
.\preflight-release.bat
```

**Erledigt?** → Schritt 5.

---

## Schritt 5 — Release-ZIP bauen & signieren

```powershell
.\build-closed-test.bat
```

Mit gesetzten Signing-Variablen signiert Gradle beim Publish automatisch. Vor dem ersten Upload prüfen:

```powershell
.\gradlew.bat signPlugin --console=plain
```

Signiertes ZIP liegt unter `build\distributions\` (Name kann `-signed` o. Ä. enthalten — siehe Ordner).

**Erledigt?** → Schritt 6.

---

## Schritt 6 — Erster Upload (Hidden + Channel)

### Variante A — Web-UI (**Pflicht für Version 0.1.0 / neues Plugin-ID**)

`upload-closed-test.bat` schlägt mit *Cannot find plugin* fehl, bis dieser Schritt einmal erledigt ist. **`build-closed-test.bat` macht keinen Upload.**

1. Optional: `setup-signing.bat` → `build-closed-test.bat` (signiertes ZIP in `build\distributions\`).
2. [Neues Plugin hochladen](https://plugins.jetbrains.com/plugin/add) (eingeloggt als Vendor **ZaYaX**).
2. ZIP wählen: `build\distributions\tabz-0.1.0-signed.zip` (nach `setup-signing.bat` + `build-closed-test.bat`).
3. **Hidden** aktivieren (wichtig — nur beim ersten Upload so möglich).
4. **Release channel:** `closed-beta` (nicht Default/Stable).
5. Texte: [MARKETPLACE-TEXT.md](MARKETPLACE-TEXT.md) · **Lizenz:** **Custom license** → Feld **License URL** (siehe unten)
6. **License URL:** öffentliche **https://…**-Adresse mit der EULA (kein `C:\…`-Pfad). Schnell: [GitHub Gist](https://gist.github.com) → Inhalt aus [TABZ-EULA.md](TABZ-EULA.md) → **Raw**-Link einfügen. Oder Repo: `https://raw.githubusercontent.com/DEIN_USER/DEIN_REPO/main/docs/TABZ-EULA.md`
7. Absenden → Warten auf **Review** durch JetBrains (typisch 1–3 Werktage).

### Variante B — Gradle

```powershell
.\upload-closed-test.bat
```

Beim allerersten Plugin ggf. trotzdem einmal Web-UI (Variante A), falls der Upload fehlschlägt.

**Erledigt?** → Schritt 7.

---

## Schritt 7 — Nach Freigabe (Approved)

1. Plugin-**Admin-Seite** öffnen (Link in der E-Mail oder unter *My Plugins*).
2. **Plugin-URL** kopieren, z. B. `https://plugins.jetbrains.com/plugin/12345-tabz`
3. Diesen Link **nur an Closed-Tester** senden (nicht ins öffentliche Repo).
4. Tester: Link öffnen → **Install to IDE**.

**Updates** später: erneut `publish-closed-test.bat` oder Upload mit **Make Hidden** — Plugin bleibt unsichtbar in der Suche, bis du **Publish Plugin** (Unhide) klickst.

---

## Schritt 8 — Öffentlicher Launch (später, optional)

Erst wenn du bereit bist: auf der Admin-Seite **Publish Plugin** → Plugin wird suchbar. **Nicht rückgängig machbar.**

---

## Schnellhilfe

| Problem | Lösung |
|--------|--------|
| `PUBLISH_TOKEN fehlt` | Schritt 3–4 |
| Upload rejected / unsigned | Schritt 2 + `signPlugin` |
| ZIP fehlt | `.\build-closed-test.bat` |
| Verifier vor Release | `prepare-jetbrains.bat`, dann `test-webstorm.bat --verify-only` |

Mehr Hintergrund: [CLOSED-TEST.md](CLOSED-TEST.md)
