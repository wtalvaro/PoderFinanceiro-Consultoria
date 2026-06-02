@echo off
setlocal enabledelayedexpansion

:: ERP Poder Financeiro - Inicializador Inteligente v@project.version@
title Poder Financeiro - v@project.version@

set "DOWNLOADS_DIR=%USERPROFILE%\Downloads"
set "TMP_DIR=%~dp0tmp"
set "APP_DIR=%~dp0"

echo [SISTEMA] Verificando novos pacotes em %DOWNLOADS_DIR%...

:: 1. Busca o ZIP mais recente que comece com PoderFinanceiro_v
set "LATEST_ZIP="
for /f "delims=" %%a in ('dir /b /a-d /o-d "%DOWNLOADS_DIR%\PoderFinanceiro_v*.zip" 2^>nul') do (
    set "LATEST_ZIP=%%a"
    goto :PROCESS_ZIP
)

:PROCESS_ZIP
if defined LATEST_ZIP (
    echo [SISTEMA] Atualizacao encontrada: %LATEST_ZIP%
    echo [SISTEMA] Preparando instalacao automatica...

    if not exist "%TMP_DIR%" mkdir "%TMP_DIR%"
    
    :: Move o arquivo para a pasta local tmp
    move /y "%DOWNLOADS_DIR%\%LATEST_ZIP%" "%TMP_DIR%\update.zip" >nul

    :: Extrai usando o tar nativo do Windows 11
    tar -xf "%TMP_DIR%\update.zip" -C "%APP_DIR%"

    if errorlevel 0 (
        echo [SISTEMA] Versao atualizada com sucesso!
        del /f /q "%TMP_DIR%\update.zip"
        rmdir "%TMP_DIR%"
    ) else (
        echo [ERRO] Falha na extração. O arquivo pode estar corrompido.
        pause
    )
)

:START_APP
echo [SISTEMA] Iniciando Poder Financeiro v@project.version@...
start "" javaw --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -jar PoderFinanceiro.jar
exit
