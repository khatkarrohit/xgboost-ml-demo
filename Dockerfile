# Use eclipse-temurin for Java 25 support
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy the Gradle wrapper and settings
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Pre-cache dependencies
RUN ./gradlew dependencies --no-daemon

# Copy the source code
COPY src src

# Build the application
RUN ./gradlew bootJar --no-daemon

# Create a slim runtime image
FROM eclipse-temurin:25-jdk
WORKDIR /app

# Install libgomp1 (required for XGBoost native libraries) and curl (for healthchecks)
RUN apt-get update && apt-get install -y libgomp1 curl && rm -rf /var/lib/apt/lists/*

# Copy the JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Run with Native Memory Tracking (NMT) enabled
# We also set a reasonable heap size to make native leaks more obvious
ENTRYPOINT ["java", "-XX:NativeMemoryTracking=summary", "-Xmx256m", "-jar", "app.jar"]
