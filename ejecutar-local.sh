#!/usr/bin/env bash
# ===========================================================================
#  Arranca la aplicación en tu computadora leyendo el archivo .env
#
#  Uso:   ./ejecutar-local.sh
#
#  Spring Boot no lee archivos .env por su cuenta. Este script carga las
#  variables en la sesión y después arranca Maven. Así no tenés que escribir
#  ninguna credencial en la línea de comandos (donde quedaría en el historial).
# ===========================================================================
set -euo pipefail

if [ ! -f .env ]; then
    echo "ERROR: no encuentro el archivo .env"
    echo
    echo "Crealo copiando el ejemplo y poniendo tus valores:"
    echo "    cp .env.example .env"
    exit 1
fi

# set -a hace que todo lo que se defina quede exportado como variable de entorno.
set -a
# shellcheck disable=SC1091
source ./.env
set +a

echo "Arrancando con el perfil 'dev'..."
echo "Cuando diga 'Started GestionVacacionesApplication', abrí http://localhost:${PORT:-8080}"
echo

exec ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
