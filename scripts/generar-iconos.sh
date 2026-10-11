#!/usr/bin/env bash
# ===========================================================================
#  Genera los íconos PNG de la aplicación instalable a partir de los SVG.
#
#  Uso:   ./scripts/generar-iconos.sh
#  Necesita rsvg-convert (paquete librsvg) e ImageMagick.
#
#  Solo hace falta correrlo si cambiás icono.svg o icono-maskable.svg.
#  Los PNG generados SÍ se versionan: la aplicación no necesita estas
#  herramientas para funcionar.
# ===========================================================================
set -euo pipefail
cd "$(dirname "$0")/../src/main/resources/static/iconos"

# Ícono normal: el que muestran el navegador y la lista de aplicaciones.
rsvg-convert -w 192 -h 192 icono.svg -o icono-192.png
rsvg-convert -w 512 -h 512 icono.svg -o icono-512.png

# Ícono "maskable": el que Android recorta según la forma de cada celular.
rsvg-convert -w 192 -h 192 icono-maskable.svg -o icono-maskable-192.png
rsvg-convert -w 512 -h 512 icono-maskable.svg -o icono-maskable-512.png

# iPhone: 180 x 180, sin transparencias (iOS redondea las esquinas solo).
rsvg-convert -w 180 -h 180 icono-maskable.svg -o apple-touch-icon.png

# Ícono de la pestaña del navegador, en 16 y 32 píxeles dentro de un .ico.
rsvg-convert -w 32 -h 32 icono.svg -o /tmp/favicon-32.png
rsvg-convert -w 16 -h 16 icono.svg -o /tmp/favicon-16.png
magick /tmp/favicon-16.png /tmp/favicon-32.png ../favicon.ico
rm -f /tmp/favicon-16.png /tmp/favicon-32.png

echo "Íconos generados."
