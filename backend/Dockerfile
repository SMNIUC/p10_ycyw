# --------------------------------------------------------------------------------------------
# Construction, puis execution : l'image finale ne contient ni sources, ni outil de compilation.
# Elle est plus petite a transferer et expose moins de surface (DA-13, ecoconception ENF-23).
# --------------------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Les dependances sont resolues avant la copie des sources : tant que le pom ne change pas, cette
# couche est reutilisee et la reconstruction ne retelecharge rien.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# --------------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

# L'application ne s'execute pas en administrateur : une faille applicative ne donne pas les
# droits d'administration du conteneur.
RUN addgroup -S ycyw && adduser -S ycyw -G ycyw
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
RUN chown -R ycyw:ycyw /app
USER ycyw

EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=10 \
    CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
