#!/bin/bash
# ERP Poder Financeiro - Inicializador Inteligente v@project.version@

DOWNLOADS_DIR="$HOME/Downloads"
APP_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$APP_DIR"

# 1. Busca o ZIP mais recente no Downloads
LATEST_ZIP=$(ls -t "$DOWNLOADS_DIR"/PoderFinanceiro_v*.zip 2>/dev/null | head -n 1)

if [ -n "$LATEST_ZIP" ]; then
    echo "[SISTEMA] Novo pacote detectado: $(basename "$LATEST_ZIP")"
    mkdir -p ./tmp
    mv "$LATEST_ZIP" ./tmp/update.zip
    
    echo "[SISTEMA] Extraindo atualização..."
    unzip -o ./tmp/update.zip -d ./
    
    rm -rf ./tmp
    echo "[SISTEMA] Atualização concluída."
fi

echo "[SISTEMA] Iniciando Poder Financeiro v@project.version@..."
java --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -jar PoderFinanceiro.jar > /dev/null 2>&1 &
disown
exit 0
