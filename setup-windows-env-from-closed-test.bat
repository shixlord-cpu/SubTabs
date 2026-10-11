@echo off
setlocal EnableExtensions
cd /d "%~dp0"

if not exist "%~dp0closed-test.env" (
    echo closed-test.env fehlt. Zuerst aus closed-test.env.example anlegen.
    pause
    exit /b 1
)

echo.
echo Setzt Windows-Benutzer-Umgebungsvariablen aus closed-test.env
echo ^(wirkt in NEUEN Terminals / IDE-Neustart — nicht in diesem Fenster^).
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-windows-env-from-closed-test.ps1"
set "EXIT_CODE=%errorlevel%"
if not "%EXIT_CODE%"=="0" pause
exit /b %EXIT_CODE%
