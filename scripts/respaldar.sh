#!/usr/bin/env bash
# ===========================================================================
#  Respalda la base de datos de producción (Neon) en un archivo de tu
#  computadora. Así el trabajo de la oficina no depende del plan gratuito de
#  nadie: si Neon desaparece mañana, los datos están en tu disco.
#
#  Uso:
#      ./scripts/respaldar.sh
#
#  Lee la conexión de .env.respaldo (ver .env.respaldo.example). Usa pg_dump
#  dentro de un contenedor de Podman: no hay que instalar PostgreSQL.
#
#  Deja el respaldo en ~/Respaldos/GestionDeVacaciones y conserva los últimos
#  30. Cada archivo se verifica al terminar: si no se puede leer, falla.
# ===========================================================================
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f .env.respaldo ]; then
    echo "ERROR: falta el archivo .env.respaldo"
    echo "Crealo a partir del ejemplo:   cp .env.respaldo.example .env.respaldo"
    exit 1
fi
set -a
# shellcheck disable=SC1091
source ./.env.respaldo
set +a
: "${URL_BASE_PRODUCCION:?Falta URL_BASE_PRODUCCION en .env.respaldo}"

CARPETA="${CARPETA_RESPALDOS:-$HOME/Respaldos/GestionDeVacaciones}"
CONSERVAR="${RESPALDOS_A_CONSERVAR:-30}"
IMAGEN="docker.io/library/postgres:17-alpine"

mkdir -p "$CARPETA"
chmod 700 "$CARPETA"   # solo vos podés leer la carpeta: tiene datos personales

NOMBRE="vacaciones_$(date +%Y-%m-%d_%H%M).dump"
DESTINO="$CARPETA/$NOMBRE"
trap 'rm -f "$DESTINO.parcial"' ERR

echo "Respaldando la base de producción..."
# La URL (con la contraseña) pasa como variable de entorno, no como argumento:
# así no aparece en la lista de procesos de tu computadora.
export URL_BASE_PRODUCCION
podman run --rm -e URL_BASE_PRODUCCION -e PGCONNECT_TIMEOUT=90 "$IMAGEN" \
    sh -c 'exec pg_dump --format=custom --no-owner --no-privileges --dbname="$URL_BASE_PRODUCCION"' \
    > "$DESTINO.parcial"

if [ ! -s "$DESTINO.parcial" ]; then
    rm -f "$DESTINO.parcial"
    echo "ERROR: el respaldo quedó vacío."
    exit 1
fi
mv "$DESTINO.parcial" "$DESTINO"
chmod 600 "$DESTINO"

echo "Verificando que el archivo se pueda leer..."
podman run --rm -v "$CARPETA:/respaldos:ro,Z" "$IMAGEN" pg_restore --list "/respaldos/$NOMBRE" > /dev/null

# Conservar solo los últimos N respaldos.
ls -1t "$CARPETA"/vacaciones_*.dump 2>/dev/null | tail -n +"$((CONSERVAR + 1))" | xargs -r rm --

echo "Listo: $DESTINO ($(du -h "$DESTINO" | cut -f1))"
echo "Respaldos guardados: $(ls -1 "$CARPETA"/vacaciones_*.dump | wc -l)"
