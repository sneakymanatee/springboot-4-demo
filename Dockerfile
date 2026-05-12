FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src ./src
RUN ./mvnw -q -DskipTests package spring-boot:repackage

ARG APP_JAR_PATTERN=target/*.jar
RUN set -e; \
	JAR_PATH=$(ls -1 ${APP_JAR_PATTERN} | grep -v '\.original$' | head -n 1); \
	test -n "$JAR_PATH"; \
	cp "$JAR_PATH" /app/app.jar

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

