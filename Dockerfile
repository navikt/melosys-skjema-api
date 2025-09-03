FROM gcr.io/distroless/java21-debian12:nonroot
LABEL maintainer="Team Melosys"
WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
