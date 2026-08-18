FROM eclipse-temurin:17-jre
LABEL authors="gkk"
WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

