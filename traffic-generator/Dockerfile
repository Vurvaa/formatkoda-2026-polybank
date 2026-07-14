FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && ./gradlew installDist --no-daemon

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S ktor && adduser -S ktor -G ktor

COPY --from=build /app/build/install/trafiic-generator /app

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 6060
USER ktor

ENTRYPOINT ["sh", "-c", "exec /app/bin/trafiic-generator $JAVA_OPTS"]