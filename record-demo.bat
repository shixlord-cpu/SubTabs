@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo.
echo ============================================================
echo  TabZ - Demo mit Session-Recording
echo ============================================================
echo.
echo Waehrend der Demo werden Editor-Zustaende nach demo-replay\ geschrieben.
echo Nach dem Schliessen der IDE: demo-replay\latest.json oder last-session.txt
echo.
call start-demo.bat %*
endlocal
