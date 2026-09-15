# ===========================================================================
#  Imagen de la aplicación, en dos etapas.
#
#  Etapa 1 (compilacion): tiene el JDK completo y Maven. Compila y arma el .jar.
#  Etapa 2 (final): solo el JRE (Java para EJECUTAR, sin compilador) y el .jar.
#
#  La imagen final no lleva código fuente, ni Maven, ni las dependencias de
#  prueba: es más chica, arranca más rápido y tiene menos cosas que atacar.
#
#  Probarla en tu computadora:
#      podman build -t gestion-vacaciones .
#      podman run --rm -p 8080:8080 --env-file .env gestion-vacaciones
# ===========================================================================

# ---------------------------------------------------------------------------
# Etapa 1: compilar
# ---------------------------------------------------------------------------
FROM docker.io/library/eclipse-temurin:21-jdk-alpine AS compilacion
WORKDIR /fuente

# Primero solo lo que define las dependencias. Mientras el pom.xml no cambie,
# esta capa queda en caché y no se vuelven a descargar en cada construcción.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Después el código. Las pruebas NO corren acá: corren en GitHub Actions en
# cada push, contra un PostgreSQL real, que acá no hay.
COPY src src
RUN ./mvnw -B -q -Dmaven.test.skip=true package \
 && cp target/gestion-vacaciones-*.jar /fuente/aplicacion.jar

# ---------------------------------------------------------------------------
# Etapa 2: ejecutar
# ---------------------------------------------------------------------------
FROM docker.io/library/eclipse-temurin:21-jre-alpine

# Nunca como root: si alguien lograra ejecutar algo dentro del contenedor, no
# tendría permisos de administrador.
RUN addgroup -S aplicacion && adduser -S -G aplicacion aplicacion
WORKDIR /aplicacion
COPY --from=compilacion --chown=aplicacion:aplicacion /fuente/aplicacion.jar aplicacion.jar
USER aplicacion

# ---------------------------------------------------------------------------
# Memoria para los 512 MB del plan gratuito de Render
# ---------------------------------------------------------------------------
#  MaxRAMPercentage=60     el heap puede usar hasta 60 % de la RAM (~300 MB);
#                          el resto queda para metaspace, hilos y el sistema.
#  UseSerialGC             el recolector de basura más liviano: con 1 CPU
#                          compartida, los recolectores paralelos no ayudan.
#  MaxMetaspaceSize=128m   tope para las clases cargadas (Spring carga muchas).
#  ReservedCodeCacheSize   tope para el código compilado en caliente.
#  Xss512k                 cada hilo reserva menos pila (Tomcat crea varios).
#  TieredStopAtLevel=1     compila rápido y no optimiza a fondo: arranca antes,
#                          que en un plan que se duerme importa más.
#  ExitOnOutOfMemoryError  si se queda sin memoria, se cierra y Render lo
#                          reinicia, en vez de quedar colgado a medias.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -Xss512k -XX:TieredStopAtLevel=1 -XX:+ExitOnOutOfMemoryError"

# Render define la variable PORT; la aplicación la lee (server.port=${PORT:8080}).
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar aplicacion.jar"]
