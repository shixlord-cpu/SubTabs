@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo.
echo ============================================================
echo  TabZ - WebStorm ^& Rider vorbereiten
echo ============================================================
echo.
echo  Laedt IDE-Archive, baut Plugin und Sandboxes, prueft den Verifier.
echo  Einmalig noetig (oder nach Aenderung von tabzIdeVersion).
echo  Dauer beim ersten Mal: oft 15-40 Minuten.
echo.
echo  Gradle-Task: prepareTabzJetBrains
echo.

call gradlew.bat --console=plain --no-configuration-cache prepareTabzJetBrains %*
set "EXIT_CODE=%errorlevel%"
if not "%EXIT_CODE%"=="0" (
    echo.
    echo Vorbereitung fehlgeschlagen ^(Exit %EXIT_CODE%^).
    pause
    endlocal
    exit /b %EXIT_CODE%
)

echo.
echo Fertig. Jetzt: test-webstorm.bat oder test-rider.bat ^(schnell^).
echo Demo: gradlew runIdeWebStorm / runIdeRider
echo.
pause
endlocal
exit /b 0
