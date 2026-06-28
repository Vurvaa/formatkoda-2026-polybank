FROM maven:3.9-eclipse-temurin-25-alpine AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:25-jre-alpine AS extractor
WORKDIR /app

COPY --from=builder /build/target/polybank.jar polybank.jar

RUN java -Djarmode=tools -jar polybank.jar extract --layers --destination /extract

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=extractor /extract/dependencies/          ./
COPY --from=extractor /extract/spring-boot-loader/    ./
COPY --from=extractor /extract/snapshot-dependencies/ ./
COPY --from=extractor /extract/application/           ./

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
USER spring

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -cp . org.springframework.boot.loader.launch.JarLauncher"]