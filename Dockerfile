FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

RUN addgroup -S spring && adduser -S spring -G spring

COPY target/polybank.jar polybank.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
USER spring

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar polybank.jar"]