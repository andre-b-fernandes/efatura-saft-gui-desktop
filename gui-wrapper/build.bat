@echo off
setlocal

set SRC_DIR=%~dp0src
set BIN_DIR=%~dp0bin

if not exist "%BIN_DIR%" mkdir "%BIN_DIR%"

javac -encoding UTF-8 -d "%BIN_DIR%" "%SRC_DIR%\EFaturaGui.java"
if errorlevel 1 (
  echo Build falhou.
  exit /b 1
)

echo Build concluido com sucesso.
