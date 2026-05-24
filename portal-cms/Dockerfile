FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# 先 COPY 所有 POM 讓 Docker 快取 dependency 層
COPY pom.xml .
COPY portal-domain/pom.xml portal-domain/
COPY portal-cms/pom.xml portal-cms/
COPY portal-web/pom.xml portal-web/
RUN mvn dependency:go-offline -pl portal-cms -am -q

COPY portal-domain/src portal-domain/src
COPY portal-cms/src portal-cms/src
RUN mvn -pl portal-cms -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
COPY --from=builder /build/portal-cms/target/portal-cms-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
