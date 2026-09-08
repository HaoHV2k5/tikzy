FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build
ARG OTEL_JAVA_AGENT_VERSION=2.20.1
COPY pom.xml .
COPY src ./src

RUN mvn -B -ntp -DskipTests package \
    && for jar in target/*.jar; do \
         case "$jar" in \
           *.original) continue ;; \
           *) cp "$jar" /build/app.jar; break ;; \
         esac; \
       done \
     && test -f /build/app.jar \
     && mvn -B -ntp dependency:get \
          -Dartifact=io.opentelemetry.javaagent:opentelemetry-javaagent:${OTEL_JAVA_AGENT_VERSION}:jar \
          -Dtransitive=false \
     && cp /root/.m2/repository/io/opentelemetry/javaagent/opentelemetry-javaagent/${OTEL_JAVA_AGENT_VERSION}/opentelemetry-javaagent-${OTEL_JAVA_AGENT_VERSION}.jar \
          /build/opentelemetry-javaagent.jar

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN groupadd --system tikzy \
    && useradd --system --gid tikzy --create-home tikzy

COPY --from=build /build/app.jar /app/app.jar
COPY --from=build /build/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
RUN chown tikzy:tikzy /app/app.jar /app/opentelemetry-javaagent.jar

USER tikzy
EXPOSE 8080 8081
ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "/app/app.jar"]
