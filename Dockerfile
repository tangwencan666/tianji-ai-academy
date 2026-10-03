FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY tj-common ./tj-common
COPY tj-pojo ./tj-pojo
COPY tj-server ./tj-server
RUN mvn -B -ntp package -DskipTests
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /app/tj-server/target/tj-server-1.0.0.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
