@echo off
setlocal

set SRC_DIR=%~dp0src
set BIN_DIR=%~dp0bin

if not exist "%BIN_DIR%" mkdir "%BIN_DIR%"

set SOURCES_FILE=%TEMP%\efatura-sources-%RANDOM%.txt
> "%SOURCES_FILE%" (
  for %%f in ("%SRC_DIR%\*.java") do (
    set "SRC_FILE=%%f"
    setlocal enabledelayedexpansion
    echo "!SRC_FILE:\=/!"
    endlocal
  )
)

javac -encoding UTF-8 -d "%BIN_DIR%" @"%SOURCES_FILE%"
set BUILD_RESULT=%ERRORLEVEL%
del "%SOURCES_FILE%" >nul 2>nul

if %BUILD_RESULT% neq 0 (
  echo Build falhou.
  exit /b 1
)

echo Build concluido com sucesso.
