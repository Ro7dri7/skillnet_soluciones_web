#!/usr/bin/env bash
# =============================================================================
# setup-ec2.sh — Preparación única del servidor EC2 (Ubuntu 22.04 LTS)
# Proyecto: SkillNet (Spring Boot 3 + Java 17)
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

echo "==> Instalando OpenJDK 17 (alineado con pom.xml: java.version=17)..."
apt-get install -y openjdk-17-jdk

echo "==> Verificando versión de Java..."
java -version

echo "==> Creando directorio de despliegue /opt/skillnet..."
mkdir -p /opt/skillnet
chown -R "${SUDO_USER:-ubuntu}:$(id -gn "${SUDO_USER:-ubuntu}")" /opt/skillnet
chmod 755 /opt/skillnet

echo "==> Preparación completada."
echo "    - Java 17 instalado"
echo "    - Directorio /opt/skillnet listo"
echo ""
echo "Próximos pasos manuales:"
echo "  1. Copiar devops/skillnet.service a /etc/systemd/system/"
echo "  2. Editar /etc/systemd/system/skillnet.service (RDS, S3, CORS Amplify, JWT, correo)"
echo "  3. sudo systemctl daemon-reload && sudo systemctl enable --now skillnet"
