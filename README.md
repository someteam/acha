# Acha-acha
## Enterprise Git Achievement solution. Web scale. In the cloud

<img src="https://dl.dropboxusercontent.com/u/561580/lj/acha.jpg" style="width: 523px; height: 403px;">

## Usage

Grab a jar [from latest release](https://github.com/someteam/acha/releases).

Run it as:

    java -jar acha-uber-0.2.0.jar
    open http://localhost:8080/

Following configuration options are supported:

    java -jar acha-uber-0.2.0.jar --ip 0.0.0.0 --port 8080 --dir .acha

## Building from source

    lein do clean, cljsbuild clean, cljsbuild once prod, uberjar
    java -jar target/acha-uber.jar &

## Development mode

    lein cljsbuild auto dev &
    lein run --reload true &
    open http://localhost:8080/index_dev.html

## License

Copyright © Nikita Prokopov, Renat Idrisov, Andrey Vasenin, Dmitry Ivanov

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
