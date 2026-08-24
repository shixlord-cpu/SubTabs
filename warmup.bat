@echo off
setlocal
cd /d "%~dp0"
echo.
echo ============================================================
echo  Einmaliger Download-Warmup (optional)
echo ============================================================
echo.
echo Laedt IntelliJ IDEA fuer das Plugin-Projekt herunter.
echo Beim allerersten Mal kann das viele Minuten dauern.
echo Danach startet start-demo.bat deutlich schneller.
echo.
call gradlew.bat initializeIntellijPlatformPlugin --console=plain --no-configuration-cache
if errorlevel 1 (
  echo Download fehlgeschlagen.
  pause
  exit /b 1
)
echo.
echo Fertig. Jetzt: start-demo.bat
echo.
pause
endlocal
