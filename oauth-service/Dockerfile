# Multi-stage Docker build for Aarogya Seva Native Google OAuth2 Microservice
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /build/target/oauth-service-*.jar /app/oauth-service.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8087

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8087} -jar /app/oauth-service.jar"]
