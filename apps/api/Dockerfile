# Runtime image only. Build the jar first (host or CI), then build the image:
#   ./gradlew bootJar
#   docker build -t yocabs-api apps/api
FROM eclipse-temurin:21-jre

RUN groupadd --system yocabs && useradd --system --gid yocabs --uid 1001 yocabs \
    && mkdir -p /var/lib/yocabs/documents \
    && chown -R yocabs:yocabs /var/lib/yocabs

WORKDIR /app
COPY --chown=yocabs:yocabs build/libs/app.jar app.jar

USER yocabs
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Kolkata"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
