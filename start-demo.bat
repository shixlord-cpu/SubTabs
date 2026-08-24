@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set GRADLE_ARGS=--console=plain --no-configuration-cache

echo.
echo ============================================================
echo  Component Subtabs - Test-IDE starten
echo ============================================================
echo.
echo WICHTIG - warum es "haengen" kann:
echo   Nach "100%% CONFIGURING" laeuft oft noch im Hintergrund:
echo   - IntelliJ IDEA Download (beim ersten Mal, ca. 1-3 GB)
echo   - Sandbox-Vorbereitung und Plugin-Build
echo   Das kann 5-20 Minuten dauern, OHNE weiteres Fortschrittsbalken-Feedback.
echo.
echo   Mit --console=plain siehst du unten einzelne Gradle-Tasks,
echo   sobald die Configure-Phase wirklich fertig ist.
echo.
echo   Schliesse zuerst eine bereits laufende Test-IDE, falls vorhanden.
echo.
echo ------------------------------------------------------------
echo [1/2] Sandbox vorbereiten (prepareSandbox)...
echo ------------------------------------------------------------
call gradlew.bat prepareSandbox %GRADLE_ARGS%
if errorlevel 1 goto :error

echo.
echo ------------------------------------------------------------
echo [2/2] Test-IDE starten (runIde)...
echo ------------------------------------------------------------
echo Ein neues IntelliJ-Fenster sollte gleich erscheinen.
echo Dieses Terminal bleibt offen, bis du die Test-IDE wieder schliessest.
echo.
call gradlew.bat runIde %GRADLE_ARGS%
if errorlevel 1 goto :error

goto :end

:error
echo.
echo ============================================================
echo  FEHLER beim Start
echo ============================================================
echo.
echo Haeufige Ursachen:
echo   1. Eine Sandbox-IDE laeuft noch (Plugin-JAR ist gesperrt)
echo      - Test-IntelliJ-Fenster schliessen und erneut versuchen
echo   2. Erster Download noch nicht fertig - einfach laenger warten
echo   3. Kein Internet / Firewall blockiert Gradle-Downloads
echo.
echo Hilfe bei gesperrter Sandbox:
echo   gradlew.bat cleanSandbox
echo   start-demo.bat
echo.
pause
exit /b 1

:end
endlocal
