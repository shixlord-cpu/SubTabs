@echo off
echo.
echo publish-closed-test.bat wurde umbenannt.
echo   Nur bauen:  build-closed-test.bat   ^(kein Upload^)
echo   Nur Upload: upload-closed-test.bat
echo.
call "%~dp0upload-closed-test.bat" %*
