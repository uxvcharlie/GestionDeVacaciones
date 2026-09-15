#!/usr/bin/env bash
# ===========================================================================
#  Restaura un respaldo en una base de datos.
#
#  ¡OJO! BORRA todo lo que haya en la base de destino y la reemplaza por el
#  contenido del respaldo. Pide confirmación escrita antes de hacer nada.
#
#  Uso:
#      URL_BASE_DESTINO='postgresql://usuario:clave@servidor/base?sslmode=require' \
#          ./scripts/restaurar.sh ~/Respaldos/GestionDeVacaciones/vacaciones_2026-09-14_1800.dump
#
#  La URL va en una variable de entorno y no como argumento, para que la
#  contraseña no quede en el historial de la terminal si la escribís con un
#  espacio adelante (en zsh y bash, así no se guarda).
# ===========================================================================
set -euo pipefail

ARCHIVO="${1:-}"
if [ -z "$ARCHIVO" ] || [ ! -f "$ARCHIVO" ]; then
    echo "Uso: URL_BASE_DESTINO='postgresql://...' $0 <archivo.dump>"
    exit 1
fi
: "${URL_BASE_DESTINO:?Definí URL_BASE_DESTINO con la base donde restaurar}"

IMAGEN="docker.io/library/postgres:17-alpine"
CARPETA="$(cd "$(dirname "$ARCHIVO")" && pwd)"
NOMBRE="$(basename "$ARCHIVO")"
SERVIDOR="$(printf '%s' "$URL_BASE_DESTINO" | sed -E 's#^[a-z]+://([^:@/]+(:[^@/]*)?@)?([^/:?]+).*#\3#')"

echo "Vas a restaurar:   $NOMBRE"
echo "En el servidor:    $SERVIDOR"
echo
echo "Esto BORRA todo lo que haya ahora en esa base y lo reemplaza por el respaldo."
read -r -p "Para continuar escribí RESTAURAR: " CONFIRMACION
if [ "$CONFIRMACION" != "RESTAURAR" ]; then
    echo "Cancelado. No se tocó nada."
    exit 1
fi

export URL_BASE_DESTINO NOMBRE
# --single-transaction: si algo falla a mitad de camino, no queda nada a medias.
podman run --rm -e URL_BASE_DESTINO -e NOMBRE -e PGCONNECT_TIMEOUT=90 -v "$CARPETA:/respaldos:ro,Z" "$IMAGEN" \
    sh -c 'exec pg_restore --clean --if-exists --no-owner --no-privileges --single-transaction --dbname="$URL_BASE_DESTINO" "/respaldos/$NOMBRE"'

echo "Listo: el respaldo quedó restaurado en $SERVIDOR."
