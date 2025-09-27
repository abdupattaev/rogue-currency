# syntax=docker/dockerfile:1

# ================
# Build stage
# ================
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline || true

# Copy sources and build
COPY src ./src
RUN mvn -q -e -DskipTests package

# ================
# Runtime stage
# ================
FROM eclipse-temurin:21-jre
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:InitialRAMPercentage=50 -XX:MaxRAMPercentage=75"
WORKDIR /app

# Copy the shaded jar produced by the shade plugin
COPY --from=builder /app/target/*-shaded.jar /app/app.jar

# Runtime env vars (Fly secrets will override)
ENV TELEGRAM_BOT_TOKEN=""
ENV TELEGRAM_BOT_USERNAME="CurrencyUzb_bot"

# No network listener needed (Telegram long-polling)
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
