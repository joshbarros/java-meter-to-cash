FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY target/account-service-1.0.0.jar app.jar
RUN groupadd --system app && useradd --system --gid app --no-create-home app && chown app:app /app/app.jar
USER app
ENV JAVA_TOOL_OPTIONS="-Xmx256m -XX:MaxMetaspaceSize=192m -XX:+UseSerialGC -Xss256k"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
