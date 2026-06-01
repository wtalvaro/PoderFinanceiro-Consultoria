@echo off
setlocal enabledelayedexpansion

:: ERP Poder Financeiro - Inicializador Gold Standard (Windows)
title Poder Financeiro - Inicializador

:: 1. Verifica se existe uma atualização pendente
if exist "update.jar" (
    echo [SISTEMA] Atualizacao detectada. Preparando instalacao...
    
    :RETRY_MOVE
    :: Tenta mover o arquivo. O Windows 11 pode demorar a liberar o lock do processo anterior.
    move /y "update.jar" "PoderFinanceiro.jar" >nul 2>&1
    
    if errorlevel 1 (
        echo [SISTEMA] Aguardando encerramento total da instancia anterior...
        timeout /t 2 /nobreak >nul
        goto :RETRY_MOVE
    )
    
    echo [SISTEMA] Atualizacao aplicada com sucesso!
)

:START_APP
echo [SISTEMA] Iniciando Poder Financeiro v2.1.5.1...

:: 2. Executa o Java 25
:: Usamos 'start' sem '/b' para o javaw se desvincular totalmente do CMD
:: --enable-native-access=ALL-UNNAMED é vital para performance de rede/IA
start "" javaw --enable-native-access=ALL-UNNAMED ^
      -Dfile.encoding=UTF-8 ^
      -jar PoderFinanceiro.jar

:: 3. Finaliza o script de lote
exit
