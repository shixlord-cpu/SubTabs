@echo off
rem ============================================================
rem  NUR Marketplace-UPLOAD (Gradle publishPlugin)
rem  Zum Bauen des ZIP: build-closed-test.bat
rem ============================================================
setlocal EnableExtensions
cd /d "%~dp0"

call "%~dp0_load-closed-test-env.bat"

if "%PUBLISH_TOKEN%"=="" (
    echo PUBLISH_TOKEN fehlt ^(closed-test.env^).
    pause
    exit /b 1
)

if not exist "build\distributions\component-subtabs-0.1.0-signed.zip" (
    if not exist "build\distributions\component-subtabs-0.1.0.zip" (
        echo Zuerst bauen: build-closed-test.bat
        pause
        exit /b 1
    )
)

echo UPLOAD zu JetBrains Marketplace ^(publishPlugin^) ...
call gradlew.bat --console=plain --no-configuration-cache ^
    -PtabzPublishHidden=true ^
    -PtabzPublishChannel=closed-beta ^
    publishPlugin %*

set "EXIT_CODE=%errorlevel%"
if not "%EXIT_CODE%"=="0" (
    echo.
    echo Erst-Upload oft nur im Browser: https://plugins.jetbrains.com/plugin/add
    pause
    exit /b %EXIT_CODE%
)
echo Upload angefordert.
pause
exit /b 0
