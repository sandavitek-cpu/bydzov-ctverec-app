# syntax=docker/dockerfile:1
# Build z kořene repa (Render default kontext = root). Backend je v backend/.
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring && \
    mkdir -p /app/logs && chown spring:spring /app/logs
USER spring:spring
COPY --from=build /app/target/bydzov-ctverec-api.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=80.0"
ENV SPRING_PROFILES_ACTIVE=production
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
