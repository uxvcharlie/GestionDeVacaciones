#!/usr/bin/env bash
# ===========================================================================
#  Compila el APK firmado, listo para mandar al celular.
#
#  Uso:   ./compilar-apk.sh
#
#  Necesita (ver README, sección "App para celular"):
#    - JDK 21 y Android SDK. Por omisión los busca en ~/Android/jdk-21 y
#      ~/Android/Sdk; se pueden cambiar con JAVA_HOME y ANDROID_HOME.
#    - El archivo firma.properties en esta carpeta, que apunta a la clave de
#      firma. Sin él, el APK sale sin firmar y Android no lo instala.
# ===========================================================================
set -euo pipefail
cd "$(dirname "$0")"

export JAVA_HOME="${JAVA_HOME:-$HOME/Android/jdk-21}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"

if [ ! -x "$JAVA_HOME/bin/java" ]; then
    echo "ERROR: no encuentro Java en $JAVA_HOME. Definí JAVA_HOME o instalalo (ver README)."
    exit 1
fi
if [ ! -d "$ANDROID_HOME/platform-tools" ]; then
    echo "ERROR: no encuentro el Android SDK en $ANDROID_HOME. Definí ANDROID_HOME (ver README)."
    exit 1
fi
if [ ! -f firma.properties ]; then
    echo "ERROR: falta firma.properties. Sin la clave de firma el celular no instala el APK."
    exit 1
fi
[ -f local.properties ] || echo "sdk.dir=$ANDROID_HOME" > local.properties

echo "Corriendo las pruebas..."
./gradlew -q :app:testDebugUnitTest
echo "Compilando la versión final..."
./gradlew -q :app:assembleRelease

VERSION=$(grep -E 'versionName *=' app/build.gradle.kts | sed -E 's/.*"(.*)".*/\1/')
DESTINO="Vacaciones-$VERSION.apk"
cp app/build/outputs/apk/release/app-release.apk "$DESTINO"

echo
echo "Listo: $(pwd)/$DESTINO ($(du -h "$DESTINO" | cut -f1))"
echo "Mandalo por WhatsApp o correo y abrilo en el celular para instalarlo."
