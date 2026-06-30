FROM maven:3.9-eclipse-temurin-25-alpine AS builder
WORKDIR /build

COPY pom.xml .
#RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B -P'!db-codegen'

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /build/target/polybank.jar polybank.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
USER spring

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar polybank.jar"]