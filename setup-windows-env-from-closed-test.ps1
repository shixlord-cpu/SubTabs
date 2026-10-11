# Sync non-empty keys from closed-test.env to Windows User environment variables.
$ErrorActionPreference = 'Stop'
$envFile = Join-Path $PSScriptRoot 'closed-test.env'
if (-not (Test-Path $envFile)) {
    Write-Error "closed-test.env not found"
    exit 1
}

$names = @('PUBLISH_TOKEN', 'PRIVATE_KEY', 'PRIVATE_KEY_PASSWORD', 'CERTIFICATE_CHAIN')
$set = 0
foreach ($line in Get-Content -LiteralPath $envFile) {
    $line = $line.Trim()
    if ($line -eq '' -or $line.StartsWith('#')) { continue }
    $eq = $line.IndexOf('=')
    if ($eq -lt 1) { continue }
    $key = $line.Substring(0, $eq).Trim()
    $val = $line.Substring($eq + 1).Trim()
    if ($val.StartsWith('"') -and $val.EndsWith('"') -and $val.Length -ge 2) {
        $val = $val.Substring(1, $val.Length - 2)
    }
    if ($names -notcontains $key) { continue }
    if ([string]::IsNullOrWhiteSpace($val)) {
        Write-Host "[skip] $key (leer)"
        continue
    }
    [Environment]::SetEnvironmentVariable($key, $val, 'User')
    Write-Host "[ok]   $key gesetzt (User)"
    $set++
}

if ($set -eq 0) {
    Write-Host "Nichts gesetzt — PUBLISH_TOKEN etc. in closed-test.env eintragen."
    exit 1
}

Write-Host ""
Write-Host "Fertig. Neues Terminal oeffnen oder IDE neu starten, damit Gradle publishPlugin die Variablen sieht."
exit 0
