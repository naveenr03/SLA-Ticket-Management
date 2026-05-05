# Build (requires network for Maven dependencies)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src ./src
RUN ./mvnw -B package -DskipTests \
    && JAR=$(ls target/*.jar | grep -v 'original' | head -1) \
    && cp "$JAR" target/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -g 1001 app && adduser -u 1001 -G app -s /bin/sh -D app
COPY --from=build /app/target/app.jar app.jar
# Non-root user must be able to create ticket attachment storage (see FileStorageService)
RUN mkdir -p /app/uploads/tickets && chown -R app:app /app
USER app
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
