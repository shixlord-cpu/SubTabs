@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "OPENSSL=C:\Program Files\Git\usr\bin\openssl.exe"
if not exist "%OPENSSL%" (
    echo OpenSSL nicht gefunden. Git for Windows installieren oder OpenSSL in PATH.
    pause
    exit /b 1
)

if not exist "certificate" mkdir certificate

if exist "certificate\private.pem" (
    echo certificate\private.pem existiert bereits. Abbruch.
    pause
    exit /b 1
)

echo Erzeuge Signing-Key ^(certificate\private.pem, chain.crt^) ...
"%OPENSSL%" genpkey -algorithm RSA -out certificate\private.pem -pkeyopt rsa_keygen_bits:4096
if errorlevel 1 goto fail
"%OPENSSL%" req -key certificate\private.pem -new -x509 -days 3650 -out certificate\chain.crt -subj "/CN=ZaYaX TabZ"
if errorlevel 1 goto fail

echo.
echo Fertig. signPlugin nutzt certificate\ automatisch ^(build.gradle.kts^).
echo Optional in closed-test.env: PRIVATE_KEY_PASSWORD= ^(leer bei unverschluesseltem Key^)
echo.
pause
exit /b 0

:fail
echo Signing-Setup fehlgeschlagen.
pause
exit /b 1
