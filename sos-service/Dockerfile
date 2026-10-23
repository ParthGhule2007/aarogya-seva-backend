# Multi-stage Docker build for Aarogya Seva SOS Emergency Alert Microservice
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy pom.xml and download dependencies first for efficient caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package application jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create a non-root application user for container security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy executable jar from builder stage
COPY --from=builder /build/target/sos-service-*.jar /app/sos-service.jar

# Adjust permissions
RUN chown -R appuser:appgroup /app
USER appuser

# Expose microservice HTTP port
EXPOSE 8082

# JVM configuration for containerized memory management
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8082} -jar /app/sos-service.jar"]
