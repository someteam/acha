# Acha-acha

Git Achievements

## Usage

    lein do clean, cljsbuild clean, cljsbuild once prod, uberjar
    java -jar target/acha-uber.jar &
    open http://localhost:8080/

Following configuration options are supported:

    java -jar target/acha-uber.jar --ip 0.0.0.0 --port 8080 --dir .acha

## Development mode

    lein cljsbuild auto dev &
    lein run --reload true &
    open http://localhost:8080/index_dev.html

## License

Copyright © Nikita Prokopov, Renat Idrisov, Andrey Vasenin, Dmitry Ivanov

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
