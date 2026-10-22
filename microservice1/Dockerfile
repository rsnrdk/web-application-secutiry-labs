# Stage 1: build the jar with the full JDK and Maven wrapper
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src src

RUN ./mvnw clean package -DskipTests

# Stage 2: run the jar on a slim JRE image
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/target/websec-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
