# Imagen del backend, en dos etapas.
#
# Se usa un Dockerfile y no la deteccion automatica del PaaS a proposito: la
# autodeteccion elige la version de Java por su cuenta, y este proyecto necesita
# 21. Un build que elige 17 falla con errores de sintaxis que no dicen nada sobre
# la version.

# ---- etapa 1: compilar -------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# El pom primero y las fuentes despues: asi la capa de dependencias se reutiliza
# entre builds mientras el pom no cambie, que es casi siempre.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src

# Sin tests aca. No es por apuro: los tests levantan un contexto de Spring con H2
# y tardan mas que el resto del build, y ya corren en la maquina antes de
# commitear. Un deploy no es el lugar donde uno se entera de que algo se rompio.
RUN mvn -B -q clean package -DskipTests

# ---- etapa 2: correr ---------------------------------------------------------
# JRE y no JDK: la imagen final no compila nada y no tiene por que cargar con un
# compilador.
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Informativo nomas: el puerto real lo decide la variable PORT que inyecta la
# plataforma, y application.yml la lee.
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
