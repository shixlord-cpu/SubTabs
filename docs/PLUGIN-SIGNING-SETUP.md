# TabZ — Plugin Signing (PRIVATE_KEY, CERTIFICATE_CHAIN)

Der **PUBLISH_TOKEN** reicht nur zum **Hochladen**. Marketplace erwartet zusätzlich ein **signiertes** ZIP (`signPlugin` vor `publishPlugin`).

## Was die drei Werte bedeuten

| Variable | Inhalt |
|----------|--------|
| `PRIVATE_KEY` | RSA-Private-Key (PEM), Datei z. B. `certificate/private.pem` |
| `PRIVATE_KEY_PASSWORD` | Passwort, mit dem der Key verschlüsselt wurde — **leer lassen**, wenn du einen unverschlüsselten `private.pem` erzeugst |
| `CERTIFICATE_CHAIN` | X.509-Zertifikat (PEM), Datei z. B. `certificate/chain.crt` |

Diese drei Werte gehören **nur** in `closed-test.env` (gitignored), **nie** in `closed-test.env.example` oder Git.

## Option A — Dateien im Ordner `certificate/` (empfohlen)

Im Projektroot (Git ignoriert `certificate/`):

### 1. Key erzeugen (Git Bash oder WSL mit OpenSSL)

```bash
openssl genpkey -aes-256-cbc -algorithm RSA -out private_encrypted.pem -pkeyopt rsa_keygen_bits:4096
openssl rsa -in private_encrypted.pem -out private.pem
```

Das Passwort von Schritt 1 → `PRIVATE_KEY_PASSWORD` in `closed-test.env`.

### 2. Zertifikat (Self-Signed, für Marketplace üblich beim Start)

```bash
openssl req -key private.pem -new -x509 -days 3650 -out chain.crt
```

Bei den Fragen reicht z. B. Common Name: `ZaYaX TabZ`.

### 3. Gradle nutzt Umgebungsvariablen **oder** du kannst in `closed-test.env` nur Pfade setzen —  
   aktuell liest `build.gradle.kts` **PEM-Inhalt** aus `PRIVATE_KEY` / `CERTIFICATE_CHAIN`.

Einfachste Variante: In `closed-test.env` die **kompletten Dateiinhalte** einfügen (mehrzeilig geht in `.env` oft schlecht) → deshalb **Option B** oder Dateien + Gradle anpassen.

**Praktisch unter Windows:** PEM-Dateien in `certificate/` legen und in PowerShell vor dem Publish:

```powershell
$env:PRIVATE_KEY = Get-Content -Raw certificate\private.pem
$env:CERTIFICATE_CHAIN = Get-Content -Raw certificate\chain.crt
$env:PRIVATE_KEY_PASSWORD = "dein-passwort"
$env:PUBLISH_TOKEN = "perm:..."
.\build-closed-test.bat
```

### 4. Public Key / Zertifikat bei JetBrains hinterlegen

Nach JetBrains-Doku ggf. öffentlichen Teil im Marketplace-Profil registrieren, sobald die Upload-UI das verlangt. Bei Ablehnung „unsigned“: Marketplace-Hilfe oder Signing-Abschnitt in der Upload-Maske prüfen.

## Option B — Nur manueller Upload (Web)

1. `.\build-closed-test.bat` → unsigned ZIP  
2. Signieren:

```powershell
# marketplace-zip-signer-cli.jar von GitHub Releases (JetBrains Marketplace ZIP Signer)
java -jar marketplace-zip-signer-cli.jar sign -in build\distributions\component-subtabs-0.1.0.zip -out build\distributions\component-subtabs-0.1.0-signed.zip -cert-file certificate\chain.crt -key-file certificate\private.pem -key-pass "DEIN_PASSWORT"
```

3. **Signed ZIP** auf [plugin/add](https://plugins.jetbrains.com/plugin/add) hochladen (Hidden + `closed-beta`).

## Test lokal

```powershell
.\gradlew.bat signPlugin --console=plain
```

Wenn Signing-Env fehlt, wird `signPlugin` übersprungen; `publishPlugin` kann dann scheitern oder Warnung erzeugen.

## Sicherheit

- **PUBLISH_TOKEN** und **private.pem** nie committen, nie in Chat posten.  
- Token im Chat? → auf [tokens](https://plugins.jetbrains.com/author/me/tokens) **widerrufen** und neu erzeugen.
