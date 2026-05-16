FROM maven:3.9-eclipse-temurin-17 AS backend-build

WORKDIR /build

COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package \
    && cp "$(find target -maxdepth 1 -name '*.jar' ! -name '*.original' | head -n 1)" /build/wiki-backend.jar

FROM eclipse-temurin:17-jre-alpine

ENV TZ=Asia/Shanghai

WORKDIR /app

RUN addgroup -S wiki \
    && adduser -S wiki -G wiki \
    && mkdir -p /app/storage/docs /app/storage/avatars /app/storage/doc-images \
    && chown -R wiki:wiki /app

COPY --from=backend-build /build/wiki-backend.jar /app/wiki-backend.jar

USER wiki

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q UP || exit 1

ENTRYPOINT ["java", "-jar", "/app/wiki-backend.jar"]
