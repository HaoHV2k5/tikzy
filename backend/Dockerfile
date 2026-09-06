FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

RUN mvn -B -ntp -DskipTests package \
    && for jar in target/*.jar; do \
         case "$jar" in \
           *.original) continue ;; \
           *) cp "$jar" /build/app.jar; break ;; \
         esac; \
       done \
    && test -f /build/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN groupadd --system tikzy \
    && useradd --system --gid tikzy --create-home tikzy

COPY --from=build /build/app.jar /app/app.jar
RUN chown tikzy:tikzy /app/app.jar

USER tikzy
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
