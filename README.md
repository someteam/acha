# Acha-acha

![Rick Hickey achievements](https://dl.dropboxusercontent.com/u/11486892/acha-hickey.png)

## Usage

Grab a jar [from latest release](https://github.com/someteam/acha/releases/latest):

    curl -L -O https://github.com/someteam/acha/releases/download/0.2.5/acha-uber-0.2.5.jar

Run it as:

    java -jar acha-uber-0.2.5.jar
    open http://localhost:8080/

Following configuration options are supported:

    java -jar acha-uber-0.2.5.jar --ip 0.0.0.0 --port 8080 --dir .acha --private-key ~/.ssh/custom_key

## Building from source

    lein do clean, cljsbuild once prod, uberjar
    java -jar target/acha-uber.jar &

## Development mode

    lein cljsbuild auto dev &
    lein run --reload &
    open http://localhost:8080/index_dev.html

## License

Copyright © Nikita Prokopov, Julie Prokopova, Renat Idrisov, Andrey Vasenin, Dmitry Ivanov

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
