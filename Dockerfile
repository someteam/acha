FROM clojure AS builder

COPY . /build
WORKDIR /build

RUN lein do clean, cljsbuild once prod, uberjar

FROM openjdk:8-jre-alpine

COPY .ssh/insecure_key /root/.ssh/id_rsa
RUN chmod 600 /root/.ssh/id_rsa
RUN printf "Host *\n\tStrictHostKeyChecking no" > /root/.ssh/config

COPY --from=builder /build/target/acha-uber.jar /app/acha-uber.jar

WORKDIR /app

EXPOSE 8080

ENTRYPOINT [ "java", "-jar", "acha-uber.jar" ]
