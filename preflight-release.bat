@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo.
echo TabZ Release Preflight
echo ======================
echo.

set "OK=1"

if exist "build\distributions\tabz-0.1.1.zip" (
    echo [OK] ZIP: build\distributions\tabz-0.1.1.zip
) else (
    echo [??] ZIP fehlt - build-closed-test.bat ausfuehren
    set "OK=0"
)

if exist "closed-test.env" (
    echo [OK] closed-test.env vorhanden
) else (
    echo [--] closed-test.env fehlt ^(Schritt 4 in docs\RELEASE-FREIGABE.md^)
)

if not "%PUBLISH_TOKEN%"=="" (
    echo [OK] PUBLISH_TOKEN in Umgebung gesetzt
) else if exist "closed-test.env" (
    echo [--] PUBLISH_TOKEN: in closed-test.env pruefen ^(upload-closed-test.bat laedt die Datei^)
) else (
    echo [--] PUBLISH_TOKEN nicht gesetzt
)

echo.
echo Naechste Schritte: docs\RELEASE-FREIGABE.md
echo.
endlocal
exit /b 0
