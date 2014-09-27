# Acha-acha

Git Achievements

## Usage

    lein uberjar
    java -jar target/acha-uber.jar &
    open http://localhost:8080/

## Development mode

    lein cljsbuild auto dev &
    lein ring server-headless 8080 &
    open http://localhost:8080/index_dev.html

## License

Copyright © Nikita Prokopov, Renat Idrisov, Andrey Vasenin, Dmitry Ivanov

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
