# Base OS Selection: Eclipse Temurin OpenJDK 17 on Debian Slim.
# Selection Criteria: Debian Slim ensures glibc compatibility for accurate JVM memory
# telemetry via ManagementFactory and robust thread management, avoiding musl libc edge cases.
FROM eclipse-temurin:17-jre-focal

# Expose the mandatory system port explicitly requested in the specification
EXPOSE 5477

# Define working directory inside the container
WORKDIR /app

# Copy the compiled executable JAR from the Maven/Gradle build target
COPY build/libs/*.jar application.jar

ENTRYPOINT ["java", "-jar", "application.jar"]