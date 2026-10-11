@echo off
setlocal EnableExtensions EnableDelayedExpansion
if "%~1"=="" (
    echo Usage: _run-ide-test.bat ^<IdeSlug^> [Gradle-Argumente...]
    echo Beispiel: _run-ide-test.bat WebStorm
    echo Slugs: Idea, WebStorm, Rider, PyCharm, PhpStorm, GoLand, CLion, RubyMine, RustRover, DataGrip, DataSpell, AndroidStudio
    exit /b 1
)

set "IDE_SLUG=%~1"
shift /1

set "EXTRA="
set "TABZ_VERIFY_ONLY=0"
set "TABZ_SKIP_VERIFY=0"
set "TABZ_RUN_IDE=0"
:collect_args
if "%~1"=="" goto args_done
if /i "%~1"=="--verify-only" (
    set "TABZ_VERIFY_ONLY=1"
    shift /1
    goto collect_args
)
if /i "%~1"=="--skip-verify" (
    set "TABZ_SKIP_VERIFY=1"
    shift /1
    goto collect_args
)
set "EXTRA=!EXTRA! %1"
shift /1
goto collect_args

:args_done
cd /d "%~dp0"

set "GRADLE_BASE=--console=plain --no-configuration-cache"
set "EXIT_CODE=0"

if /i "!IDE_SLUG!"=="Idea" (
    set "GRADLE_TASK=test"
    if "!EXTRA!"=="" (
        set "EXTRA= --tests com.zayax.tabz.SubtabStackRulesTest --tests com.zayax.tabz.CustomSubtabRuleMatcherTest --tests com.zayax.tabz.TabzSettingsTest"
    )
) else if /i "!IDE_SLUG!"=="WebStorm" (
    set "GRADLE_CHECK=checkTabzWebStorm"
    set "GRADLE_RUN=runIdeWebStorm"
    set "VERIFY_PROP=-PtabzVerifyOnly=WebStorm"
    set "TABZ_RUN_IDE=1"
) else if /i "!IDE_SLUG!"=="Rider" (
    set "GRADLE_CHECK=checkTabzRider"
    set "GRADLE_RUN=runIdeRider"
    set "VERIFY_PROP=-PtabzVerifyOnly=Rider"
    set "TABZ_RUN_IDE=1"
) else (
    set "GRADLE_TASK=verifyTabz!IDE_SLUG!"
    set "VERIFY_PROP="
)

if "!TABZ_VERIFY_ONLY!"=="1" set "TABZ_RUN_IDE=0"

echo.
echo ============================================================
echo  TabZ - IDE: !IDE_SLUG!
echo ============================================================
if /i "!IDE_SLUG!"=="Idea" (
    echo  Gradle: test!EXTRA!
) else if "!TABZ_RUN_IDE!"=="1" (
    echo  Ablauf: Plugin-Check, dann Sandbox-IDE starten ^(!GRADLE_RUN!^)
    echo  Nur Verifier: --verify-only   Nur IDE ^(ohne Check^): --skip-verify
    echo  Erstes Mal langsam? prepare-jetbrains.bat
) else (
    echo  Gradle: !GRADLE_TASK!
)
echo  IDE-Version: tabzIdeVersion ^(Standard 2025.3^)
echo.

if not "!TABZ_RUN_IDE!"=="1" (
    call gradlew.bat !GRADLE_BASE! !VERIFY_PROP! !GRADLE_TASK!!EXTRA!
    set "EXIT_CODE=!errorlevel!"
    goto tabz_finish
)

if not "!TABZ_SKIP_VERIFY!"=="1" (
    echo ------------------------------------------------------------
    echo  [1/2] Plugin gegen !IDE_SLUG! pruefen ^(!GRADLE_CHECK!^)
    echo ------------------------------------------------------------
    call gradlew.bat !GRADLE_BASE! !VERIFY_PROP! !GRADLE_CHECK!!EXTRA!
    set "EXIT_CODE=!errorlevel!"
    if not "!EXIT_CODE!"=="0" goto tabz_finish
)

echo.
echo ------------------------------------------------------------
echo  [2/2] !IDE_SLUG! mit TabZ starten ^(!GRADLE_RUN!^)
echo ------------------------------------------------------------
echo  Ein !IDE_SLUG!-Fenster sollte gleich erscheinen.
echo  Terminal bleibt offen, bis du die Sandbox-IDE schliessest.
echo.
call gradlew.bat !GRADLE_BASE! !GRADLE_RUN!!EXTRA!
set "EXIT_CODE=!errorlevel!"

:tabz_finish
if not "!EXIT_CODE!"=="0" (
    echo.
    echo Fehlgeschlagen ^(Exit !EXIT_CODE!^).
    echo Tipp: prepare-jetbrains.bat, Sandbox-IDE schliessen, oder gradlew cleanSandbox
    pause
    endlocal
    exit /b !EXIT_CODE!
)
endlocal
exit /b 0
