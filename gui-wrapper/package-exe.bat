@echo off
setlocal

where jpackage >nul 2>nul
if errorlevel 1 (
  echo jpackage nao foi encontrado no PATH. A tentar carregar variaveis de .env...
  call "%~dp0load-env.cmd"

  where jpackage >nul 2>nul
  if errorlevel 1 (
    echo jpackage continua indisponivel apos carregar .env.
    echo Instale um JDK que inclua jpackage e adicione a pasta bin ao PATH.
    echo Exemplo de validacao: jpackage --version
    exit /b 1
  )
)

call "%~dp0build.bat"
if errorlevel 1 exit /b 1

set DIST_DIR=%~dp0dist
set RELEASE_DIR=%~dp0release
set APP_JAR=EFaturaGuiWrapper.jar

if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"
if not exist "%RELEASE_DIR%" mkdir "%RELEASE_DIR%"

set "WIX_OK=0"
where wix >nul 2>nul && set "WIX_OK=1"
where candle >nul 2>nul && where light >nul 2>nul && set "WIX_OK=1"

if "%WIX_OK%"=="0" (
  echo WiX Toolset nao encontrado.
  echo Para gerar --type exe no Windows, instale WiX e adicione ao PATH.
  echo Site: https://wixtoolset.org
  exit /b 1
)

jar --create --file "%DIST_DIR%\%APP_JAR%" --main-class EFaturaGui -C "%~dp0bin" .
if errorlevel 1 (
  echo Falha ao criar jar da aplicacao.
  exit /b 1
)

if exist "%~dp0..\FACTEMICLI-2.9.1-100067-cmdClient.jar" (
  copy /Y "%~dp0..\FACTEMICLI-2.9.1-100067-cmdClient.jar" "%DIST_DIR%\FACTEMICLI-2.9.1-100067-cmdClient.jar" >nul
)

jpackage ^
  --type exe ^
  --name EFaturaGuiWrapper ^
  --app-version 1.0.0 ^
  --vendor "EFatura Local" ^
  --input "%DIST_DIR%" ^
  --main-jar "%APP_JAR%" ^
  --dest "%RELEASE_DIR%" ^
  --win-dir-chooser ^
  --win-menu ^
  --win-shortcut

if errorlevel 1 (
  echo Falha ao gerar .exe.
  exit /b 1
)

echo Instalador gerado com sucesso em:
echo %RELEASE_DIR%
