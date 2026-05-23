FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appuser && adduser -S appuser -G appuser

COPY --from=builder /build/target/*.jar /app/app.jar

USER appuser

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS=""

ENTRYPOINT ["java", "-jar", "/app/app.jar"]