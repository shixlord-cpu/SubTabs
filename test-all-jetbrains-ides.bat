@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
set GRADLE_ARGS=--console=plain --no-configuration-cache

echo.
echo ============================================================
echo  TabZ - alle JetBrains IDEs
echo ============================================================
echo  [1/2] IntelliJ IDEA: Unit-Schnelltest
echo  [2/2] Weitere IDEs: Plugin Verifier ^(verifyTabz*^)
echo.

call gradlew.bat %GRADLE_ARGS% test --tests de.sasbe.subtabs.SubtabStackRulesTest --tests de.sasbe.subtabs.CustomSubtabRuleMatcherTest --tests de.sasbe.subtabs.SubtabsSettingsTest
if errorlevel 1 goto failed

set "FAILED=0"
call gradlew.bat %GRADLE_ARGS% -PtabzVerifyOnly=WebStorm checkTabzWebStorm
if errorlevel 1 set "FAILED=1"
call gradlew.bat %GRADLE_ARGS% -PtabzVerifyOnly=Rider checkTabzRider
if errorlevel 1 set "FAILED=1"

for %%I in (PyCharm PhpStorm GoLand CLion RubyMine RustRover DataGrip DataSpell AndroidStudio) do (
    echo.
    echo ---- verifyTabz%%I ----
    call gradlew.bat %GRADLE_ARGS% verifyTabz%%I
    if errorlevel 1 set "FAILED=1"
)

if "!FAILED!"=="1" goto failed
echo.
echo Alle IDE-Checks erfolgreich.
endlocal
exit /b 0

:failed
echo.
echo Mindestens ein IDE-Check ist fehlgeschlagen.
pause
endlocal
exit /b 1
