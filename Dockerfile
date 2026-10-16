# =============================================================================
#  MULTIMÉTRICO API — imagen de la aplicación
#
#  Build en dos etapas: la primera compila con Maven, la segunda sólo lleva
#  el JRE y las capas del jar. La imagen final no incluye Maven ni el código.
#
#  Construir:  docker build -t multimetrico-api .
#
#  Se usa un Dockerfile y no la deteccion automatica del PaaS a proposito: la
#  autodeteccion elige la version de Java por su cuenta, y este proyecto
#  necesita 21. Un build que elige 17 falla con errores de sintaxis que no
#  dicen nada sobre la version.
# =============================================================================


# -----------------------------------------------------------------------------
#  ETAPA 1 — compilación
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Las dependencias se descargan en una capa aparte: mientras el pom.xml no
# cambie, Docker la reutiliza y el build siguiente es mucho más rápido.
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn clean package -DskipTests -B -q

# El jar se separa en las capas de Spring Boot. Las dependencias casi nunca
# cambian y quedan en una capa propia: un deploy que sólo toca código mueve
# unos cientos de KB y no el jar entero.
RUN cp target/*.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --destination extraido


# -----------------------------------------------------------------------------
#  ETAPA 2 — ejecución
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Usuario sin privilegios, con UID fijo y numérico: Kubernetes (runAsNonRoot)
# y Podman lo verifican por número. /app/tmp es lo único en lo que escribe.
RUN groupadd --system --gid 10001 app \
 && useradd --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app \
 && mkdir -p /app/tmp \
 && chown app:app /app/tmp

# Las capas quedan de root: el usuario de la app las lee pero no puede
# reescribirlas si alguien logra ejecutar código adentro.
COPY --from=build /build/extraido/dependencies/ ./
COPY --from=build /build/extraido/spring-boot-loader/ ./
COPY --from=build /build/extraido/snapshot-dependencies/ ./
COPY --from=build /build/extraido/application/ ./

USER 10001

EXPOSE 8082

# La JVM toma la zona de TZ; se puede pisar al correr sin reconstruir.
ENV TZ=UTC

# Techo de memoria explícito, no un porcentaje del host: Railway factura la RAM
# que el proceso tiene tomada y el recolector no devuelve lo que ya no usa.
# Son los valores del perfil de recursos bajo; en Railway el perfil del
# proyecto pisa esta variable entera.
ENV JAVA_TOOL_OPTIONS="-Xmx256m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=64m -XX:TieredStopAtLevel=1 -Xss512k -XX:MaxDirectMemorySize=64m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

# Lo que no es memoria va en otra variable, justamente porque Railway reemplaza
# JAVA_TOOL_OPTIONS completa: si el directorio temporal viviera ahí, el perfil
# se lo llevaría puesto. Las subidas multipart de Tomcat se escriben acá.
ENV JDK_JAVA_OPTIONS="-Djava.io.tmpdir=/app/tmp"

# Railway no lee esta directiva (usa healthcheckPath) y `podman build` en
# formato OCI la descarta; sirve para `docker run` y para Compose sin
# healthcheck propio. PORT se expande al correr, no al construir.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -qO- "http://127.0.0.1:${PORT:-8082}/actuator/health" | grep -q UP || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
