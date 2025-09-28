# ---- Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests clean package

# ---- Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
# If you use the shade plugin, this will be a single fat jar:
COPY --from=build /workspace/target/*.jar /app/app.jar
# If your jar name is fixed, adjust the COPY accordingly.

# (Optional) Health: let Fly see the process is alive
HEALTHCHECK --interval=30s --timeout=3s \
  CMD pgrep -f "app.jar" >/dev/null || exit 1

CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]