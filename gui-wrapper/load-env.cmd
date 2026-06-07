@echo off
set "ENV_FILE=%~dp0.env"
if not exist "%ENV_FILE%" (
  echo Env file not found: %ENV_FILE%
  exit /b 1
)

for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
  if not "%%A"=="" (
    call set "%%A=%%B"
    echo Set %%A
  )
)

echo Environment variables loaded from %ENV_FILE%
exit /b 0
