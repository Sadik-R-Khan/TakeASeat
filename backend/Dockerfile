# Stage 1: Build the Spring Boot application
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy built JAR from build stage
COPY --from=build /app/target/takeaseat-backend-*.jar app.jar

# Expose backend port
EXPOSE 8080

# Run Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]