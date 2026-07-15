FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon dependencies || true
COPY . .
RUN ./gradlew installDist --no-daemon

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S ktor && adduser -S ktor -G ktor
COPY --from=build --chown=ktor:ktor /app/build/install/traffic-generator /app
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 6060
USER ktor
ENTRYPOINT ["/app/bin/traffic-generator"]