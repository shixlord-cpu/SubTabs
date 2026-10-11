@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo.
echo ============================================================
echo  TabZ - Plugin bauen ^(NUR Build, KEIN Upload^)
echo ============================================================
echo  Ausgabe: build\distributions\
echo  Optional signiert, wenn certificate\ existiert ^(setup-signing.bat^).
echo  Marketplace-Upload: separat upload-closed-test.bat oder Browser.
echo.

call gradlew.bat --console=plain --no-configuration-cache prepareClosedTestRelease %*
set "EXIT_CODE=%errorlevel%"
if not "%EXIT_CODE%"=="0" (
    pause
    exit /b %EXIT_CODE%
)

echo.
echo Fertig. ZIP: build\distributions\
echo Install: IDE - Install Plugin from Disk ... ^(kein Upload durch diese BAT^).
echo.
pause
endlocal
exit /b 0
