# TabZ — Closed Test (nicht öffentlich im Marketplace)

Ziel: **Beta-Tester** können TabZ installieren und Updates bekommen, ohne dass das Plugin in der Marketplace-Suche oder in der IDE-Suche erscheint.

Plugin-ID: `com.zayax.component-subtabs`

---

## Empfohlener Weg: JetBrains Marketplace **Hidden**

JetBrains unterstützt **versteckte** Plugins und Updates ([Hidden release](https://plugins.jetbrains.com/docs/marketplace/hidden-plugin.html)):

- **Nicht** in Marketplace-Suche, IDE-Plugin-Suche oder Suchmaschinen (solange hidden).
- **Erreichbar** über **direkten Link** zur Plugin-Seite (Link nur an Tester weitergeben).
- Beim **allerersten Upload** Hidden aktivieren (nachträglich „Plugin verstecken“ geht nicht — nur Updates einzeln hidden/unhidden).

### Ablauf für dich (einmalig)

1. **JetBrains-Konto** und Vendor anlegen ([vendor/new](https://plugins.jetbrains.com/vendor/new) oder beim [Plugin-Upload](https://plugins.jetbrains.com/plugin/add); `/vendor/edit` erst nach existierendem Vendor).
2. **Plugin Signing** einrichten ([Anleitung](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)) — Zertifikat + Private Key.
3. **Publish-Token** erzeugen: Marketplace → Profil → **Tokens** → neues Token mit Upload-Recht.
4. Secrets lokal setzen (nicht ins Git), siehe `closed-test.env.example`:
   - `PUBLISH_TOKEN`
   - `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`, `CERTIFICATE_CHAIN`
5. Release bauen und hochladen:
   ```powershell
   .\upload-closed-test.bat
   ```
   Das setzt `-PtabzPublishHidden=true` und Channel **`closed-beta`**.

6. **Erster Upload per Web-UI** (falls Gradle-Upload beim allersten Mal scheitert):
   - ZIP aus `build\distributions\` (nach `signPlugin` / `buildPlugin`) manuell hochladen.
   - **Hidden** ankreuzen.
   - Channel **`closed-beta`** wählen.

7. Nach Freigabe durch JetBrains: **Plugin-URL** kopieren (z. B. `https://plugins.jetbrains.com/plugin/…`) — das ist dein **Einladungslink** für Tester.

### Öffentlicher Launch später

Auf der Plugin-Admin-Seite **Publish Plugin** → Plugin wird sichtbar. **Unhide ist endgültig** — erst wenn du wirklich live gehen willst.

---

## Tester: Installation über Marketplace (empfohlen)

1. Einladungslink öffnen (von dir per Mail/Chat).
2. Auf der Seite **Install to IDE** wählen (oder in der IDE: Settings → Plugins → Marketplace-Tab mit Link).
3. Optional **Custom Plugin Repository** für Updates auf dem Beta-Channel:
   - Settings → Plugins → Zahnrad → **Manage Plugin Repositories…**
   - URL: `https://plugins.jetbrains.com/plugins/closed-beta/list`  
     (nur nötig, wenn Tester Updates aus dem Channel `closed-beta` automatisch bekommen sollen; Hidden-Plugin-Seite reicht oft für Install + Update über dieselbe Listing-Seite.)

**Hinweis:** Es gibt **keine eingebauten Einladungs-Codes** im Marketplace für kostenlose Plugins. Zugangskontrolle = **Link geheim halten** + optional Channel-URL nur an Tester. Wer den Link hat, kann installieren.

---

## Alternative: ZIP nur für vertraute Tester

Ohne Marketplace (schnell, keine Review-Wartezeit):

```powershell
.\build-closed-test.bat
```

Tester: **Settings → Plugins → Install Plugin from Disk…** → ZIP aus `build\distributions\`.

Nachteile: **keine automatischen Updates** über Marketplace; Version manuell weitergeben.

---

## Gradle / Batch im Projekt

| Datei / Task | Zweck |
|--------------|--------|
| `build-closed-test.bat` | ZIP bauen (`prepareClosedTestRelease`) |
| `build-closed-test.bat` | **Nur** Plugin-ZIP bauen (signiert wenn `certificate\` da) |
| `upload-closed-test.bat` | **Nur** Gradle-Upload (`publishPlugin`) |
| `prepareClosedTestRelease` | Gradle: `buildPlugin` + Hinweis auf ZIP-Pfad |
| `publishPlugin` | Upload (Token + Signing nötig) |

Properties:

- `-PtabzPublishHidden=true` — Update als hidden markieren
- `-PtabzPublishChannel=closed-beta` — Custom Channel (Standard)

---

## Checkliste vor dem ersten Closed Test

- [ ] `.\gradlew.bat buildPlugin` / Verifier grün (`prepare-jetbrains.bat` bei Bedarf)
- [ ] Marketplace-Seite: Beschreibung, Screenshots, Supported IDEs
- [ ] `sinceBuild` / `untilBuild` in `build.gradle.kts` passen zur Ziel-IDE-Version
- [ ] Tester-Kanal festlegen (E-Mail, Discord, …) für Feedback
- [ ] Einladungslink **nicht** in README oder öffentliches Repo posten

---

## FAQ

**Wird das Plugin bei einem Update automatisch öffentlich?**  
Nein. Solange das Listing **hidden** ist und du nicht **Publish Plugin** (Unhide) klickst, bleibt es aus der Suche weg. Updates kannst du weiter mit **Make Hidden** bzw. `upload-closed-test.bat` (`-PtabzPublishHidden=true`) einspielen.

**Kann ich WebStorm/Rider-Tester ohne Lizenz über Marketplace testen lassen?**  
Marketplace-Install ist unabhängig von der IDE-Lizenz; die **IDE** verlangt weiterhin Trial/Lizenz beim Start.

**Stable und Closed Test parallel?**  
Später: Stable auf Default-Channel veröffentlichen, Beta weiter auf `closed-beta`. Custom Channels sind nach Stable-Veröffentlichung für alle sichtbar (Tabs auf der Plugin-Seite) — für strikt geheime Betas vor Launch nur **Hidden** nutzen.
