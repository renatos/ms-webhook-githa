#!/bin/bash
# Script para realizar o build nativo na VM para o Microserviço de Webhooks
# Este script deve ser executado no diretório /opt/ms-webhook-githa/source

set -e

echo "=== Iniciando Build Nativo do ms-webhook-githa ==="

# 1. Garantir permissões de execução para o gradlew
chmod +x ./gradlew

# 2. Executar o build nativo usando container
echo "Construindo binário nativo... (Isso pode levar alguns minutos)"
./gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=true -Dquarkus.native.container-runtime=docker -x test

echo "=== Build concluído com sucesso! ==="

# 3. Reiniciar os containers no diretório raiz do microserviço
echo "Reiniciando containers com a nova versão nativa..."
cd /opt/ms-webhook-githa
docker compose -f docker-compose.native.yml up -d --build

echo "=== Deploy Nativo finalizado! ==="
docker image prune -f
