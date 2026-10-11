@echo off
rem Laedt closed-test.env (gitignored). tokens=1* damit Werte mit "=" funktionieren (Publish-Token).
if not exist "%~dp0closed-test.env" exit /b 0
for /f "usebackq eol=# tokens=1* delims==" %%A in ("%~dp0closed-test.env") do (
    if not "%%A"=="" set "%%A=%%B"
)
exit /b 0
