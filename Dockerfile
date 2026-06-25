# Stage 1: Build the application jar using Maven
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy all source files and compile
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create a minimal JRE runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the compiled jar from the build stage
COPY --from=build /app/target/payrollmanagement-0.0.1-SNAPSHOT.jar app.jar

# Create directory for persistent salary slips storage
RUN mkdir -p salary-slips

# Expose HTTP port
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
