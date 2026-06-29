#!/usr/bin/env bash
# =============================================================================
# deploy.sh — CI/CD básico desde máquina local (Git Bash / Linux / macOS)
# Proyecto: SkillNet (Spring Boot 3)
#
# Compila el backend, transfiere el .jar vía SCP y reinicia el servicio systemd.
#
# Uso:
#   ./devops/deploy.sh <ruta-a-llave.pem> <usuario@host-ec2>
#
# Ejemplo:
#   ./devops/deploy.sh ~/.ssh/skillnet-prod.pem ubuntu@54.123.45.67
# =============================================================================

set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "Uso: $0 <ruta-a-llave.pem> <usuario@host-ec2>"
  echo "Ejemplo: $0 ~/.ssh/skillnet-prod.pem ubuntu@54.123.45.67"
  exit 1
fi

PEM_KEY="$1"
EC2_TARGET="$2"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKEND_DIR="${PROJECT_ROOT}/backend"
JAR_NAME="skillnet-backend-0.0.1-SNAPSHOT.jar"
LOCAL_JAR="${BACKEND_DIR}/target/${JAR_NAME}"
REMOTE_TMP="/tmp/backend.jar"
REMOTE_DEST="/opt/skillnet/backend.jar"
SERVICE_NAME="skillnet.service"

if [[ ! -f "${PEM_KEY}" ]]; then
  echo "Error: no se encontró la llave PEM en ${PEM_KEY}"
  exit 1
fi

chmod 400 "${PEM_KEY}"

SSH_OPTS=(-i "${PEM_KEY}" -o StrictHostKeyChecking=accept-new)
SCP_OPTS=(-i "${PEM_KEY}" -o StrictHostKeyChecking=accept-new)

echo "==> Compilando backend (Maven, sin tests)..."
cd "${BACKEND_DIR}"
mvn clean package -DskipTests

if [[ ! -f "${LOCAL_JAR}" ]]; then
  echo "Error: no se generó ${LOCAL_JAR}"
  exit 1
fi

echo "==> Transfiriendo .jar a ${EC2_TARGET}..."
scp "${SCP_OPTS[@]}" "${LOCAL_JAR}" "${EC2_TARGET}:${REMOTE_TMP}"

echo "==> Instalando artefacto y reiniciando servicio..."
ssh "${SSH_OPTS[@]}" "${EC2_TARGET}" bash -s <<EOF
set -euo pipefail
sudo mkdir -p /opt/skillnet
sudo mv ${REMOTE_TMP} ${REMOTE_DEST}
sudo chown ubuntu:ubuntu ${REMOTE_DEST}
sudo chmod 644 ${REMOTE_DEST}
sudo systemctl daemon-reload
sudo systemctl restart ${SERVICE_NAME}
sudo systemctl status ${SERVICE_NAME} --no-pager -l
EOF

echo "==> Despliegue SkillNet completado."
