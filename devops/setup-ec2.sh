#!/usr/bin/env bash
# =============================================================================
# setup-ec2.sh — Preparación única del servidor EC2 (Ubuntu 22.04 LTS)
# Proyecto: Lernymart (Spring Boot 3)
#
# Uso (dentro de la instancia EC2, como root o con sudo):
#   chmod +x setup-ec2.sh
#   sudo ./setup-ec2.sh
# =============================================================================

set -euo pipefail

echo "==> Actualizando repositorios y paquetes del sistema..."
export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get upgrade -y

echo "==> Instalando OpenJDK 17..."
apt-get install -y openjdk-17-jdk

echo "==> Verificando versión de Java..."
java -version

echo "==> Creando directorio de despliegue /opt/lernymart..."
mkdir -p /opt/lernymart
chown -R "${SUDO_USER:-ubuntu}:$(id -gn "${SUDO_USER:-ubuntu}")" /opt/lernymart
chmod 755 /opt/lernymart

echo "==> Preparación completada."
echo "    - Java 17 instalado"
echo "    - Directorio /opt/lernymart listo"
echo ""
echo "Próximos pasos manuales:"
echo "  1. Copiar devops/lernymart.service a /etc/systemd/system/"
echo "  2. Crear /etc/lernymart/lernymart.env con credenciales RDS, S3 y correo"
echo "  3. sudo systemctl daemon-reload && sudo systemctl enable --now lernymart"
