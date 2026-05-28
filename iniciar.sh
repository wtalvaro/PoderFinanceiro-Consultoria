#!/bin/bash
# ERP Poder Financeiro - Inicializador Linux
cd "$(dirname "$0")"
echo "Iniciando ERP Poder Financeiro v2.1.0..."
java -jar PoderFinanceiro.jar &
