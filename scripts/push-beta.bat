@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0push-beta.ps1" %*
endlocal
