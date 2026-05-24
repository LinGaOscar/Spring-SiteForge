FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY portal-domain/pom.xml portal-domain/
COPY portal-cms/pom.xml portal-cms/
COPY portal-web/pom.xml portal-web/
RUN mvn dependency:go-offline -pl portal-web -am -q

COPY portal-domain/src portal-domain/src
COPY portal-web/src portal-web/src
RUN mvn -pl portal-web -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
COPY --from=builder /build/portal-web/target/portal-web-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
