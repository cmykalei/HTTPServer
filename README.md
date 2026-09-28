# HTTP Server
A simple HTTP server implementation in Java.

## Project 🌳
```
.
└── src
    ├── localhost
    │   ├── index.html
    │   └── theThinker1.jpg
    └── main
        └── java
            ├── HttpServer.java
            ├── HttpServerRequest.java
            ├── HttpServerSession.java
            └── Protocol.java
```

### Usage
Compile and start the server from the `src/main/java` directory:
```bash
javac src/main/java/HttpServer.java
java -cp src/main/java HttpServer
```

The server serves files from the `src/localhost/` directory.

## Commands
1. To start the server use `java -cp src/main/java HttpServer`.
