#!/bin/bash
# ERP Poder Financeiro - Inicializador Universal Linux/macOS
# Mantém paridade com a lógica de Hot Swap do Windows

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$APP_DIR"

JAR_NAME="PoderFinanceiro.jar"
UPDATE_NAME="update.jar"

# 1. Verifica se existe uma atualização pendente
if [ -f "$UPDATE_NAME" ]; then
    echo "[SISTEMA] Aplicando atualização v2.1.5..."
    mv -f "$UPDATE_NAME" "$JAR_NAME"
fi

# 2. Executa o Java 25 com otimizações de acesso nativo
# O uso do '&' e 'disown' permite fechar o terminal sem encerrar o ERP
echo "[SISTEMA] Iniciando Poder Financeiro..."
java --enable-native-access=ALL-UNNAMED \
     -Dfile.encoding=UTF-8 \
     -jar "$JAR_NAME" > /dev/null 2>&1 &

disown
exit 0
