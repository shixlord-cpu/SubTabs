@echo off
rem Nur Rider-Sandbox starten (ohne Plugin-Verifier)
call "%~dp0_run-ide-test.bat" Rider --skip-verify %*
