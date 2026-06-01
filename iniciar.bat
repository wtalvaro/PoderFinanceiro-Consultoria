@echo off
setlocal enabledelayedexpansion

:: 1. Verifica se existe uma atualização pendente
if exist "update.jar" (
    echo [SISTEMA] Aplicando atualizacao v2.1....
    move /y "update.jar" "PoderFinanceiro.jar" >nul
    :: O comando 'goto' após o move ajuda o CMD a não perder o ponteiro
    goto :START_APP
)

:START_APP
echo [SISTEMA] Iniciando Poder Financeiro...

:: 2. Executa o Java 25 com permissões de acesso nativo (remove os warnings do log)
:: O uso do 'start /b' permite que o script feche sem matar o processo Java
start /b "" javaw --enable-native-access=ALL-UNNAMED -jar PoderFinanceiro.jar

:: 3. Finaliza o script de lote imediatamente para evitar o erro de "arquivo não encontrado"
exit
