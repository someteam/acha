![Rick Hickey achievements](https://raw.githubusercontent.com/someteam/acha/master/hickey.png)

## Usage

If you have a docker use the following command to boot acha-acha on localhost:

    docker run --rm -p 8080:8080 someteam/acha

After that open http://localhost:8080/ to see acha-acha in your browser

### Custom private key

If you want to use acha-acha for private repositories you need to provide the private key with read access:

    docker run --rm -p 8080:8080 -v /path/to/private/id_rsa:/root/.ssh/id_rsa someteam/acha

### Mount a custom storage

If you want to store acha-acha state outside the container, use the following command to mount external working directory:

    docker run --rm -p 8080:8080 -v /path/to/working_dir:/app/.acha someteam/acha

## Building from source

    lein do clean, cljsbuild once prod, uberjar
    java -jar target/acha-uber.jar --ip 0.0.0.0 --port 8080 --dir .acha --private-key ~/.ssh/custom_key

## Development mode

    lein cljsbuild auto dev &
    lein run --reload &
    open http://localhost:8080/index_dev.html

## License

Copyright © Nikita Prokopov, Julie Prokopova, Renat Idrisov, Andrey Vasenin, Dmitry Ivanov

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
