# Acha-acha

![Rick Hickey achievements](https://dl.dropboxusercontent.com/u/11486892/acha-hickey.png)

## Usage

Grab a jar [from latest release](https://github.com/someteam/acha/releases/latest).

Run it as:

    curl -L -o acha-uber.jar https://github.com/someteam/acha/releases/download/0.2.1/acha-uber-0.2.1.jar
    java -jar acha-uber.jar
    open http://localhost:8080/

Following configuration options are supported:

    java -jar acha-uber.jar --ip 0.0.0.0 --port 8080 --dir .acha

## Building from source

    lein do clean, cljsbuild clean, cljsbuild once prod, uberjar
    java -jar target/acha-uber.jar &

## Development mode

    lein cljsbuild auto dev &
    lein run --reload true &
    open http://localhost:8080/index_dev.html

## License

Copyright © Nikita Prokopov, Yulya Prokopova, Renat Idrisov, Andrey Vasenin, Dmitry Ivanov

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
