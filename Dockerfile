FROM clojure AS builder

COPY . /build
WORKDIR /build

RUN lein do clean, cljsbuild once prod, uberjar


FROM openjdk:8-jre-alpine

COPY --from=builder /build/target/acha-uber.jar /app/acha-uber.jar
WORKDIR /app

EXPOSE 8080

ENTRYPOINT [ "java", "-jar", "acha-uber.jar" ]
