FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /opt/app/data \
    && chown -R app:app /opt/app

WORKDIR /opt/app
COPY target/ipinfo-local-service-1.0.0.jar app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
