FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY backend ./backend
COPY frontend ./frontend
RUN chmod +x ./backend/mvnw \
    && ./backend/mvnw -B -f backend/pom.xml package \
    && jar_path="$(find backend/target -maxdepth 1 -type f -name 'student-productivity-*.jar' ! -name '*.jar.original' -print -quit)" \
    && test -n "$jar_path" \
    && cp "$jar_path" /workspace/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/app.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
