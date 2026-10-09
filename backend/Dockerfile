FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV TZ=UTC
EXPOSE 8080
# the free instance has little RAM; the default heap would only be 25% of it
ENTRYPOINT ["java","-XX:MaxRAMPercentage=65","-XX:+UseSerialGC","-Xss512k","-jar","app.jar"]