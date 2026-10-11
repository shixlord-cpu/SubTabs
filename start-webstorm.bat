@echo off
rem Nur WebStorm-Sandbox starten (ohne Plugin-Verifier)
call "%~dp0_run-ide-test.bat" WebStorm --skip-verify %*
