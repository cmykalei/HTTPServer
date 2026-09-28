import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.FileNotFoundException;

public class HttpServerSession extends Thread {

    private Socket socket;
    private volatile boolean running = true;
    public String name;

    public HttpServerSession(Socket socket) {
        this.socket = socket;
        try {
            name = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            System.err.println(e.getMessage());
            name = "HttpServerSession";
        }
    }

    private void send(BufferedOutputStream out, byte[] response) {
        try {
            out.write(response);
            out.flush();
        } catch (IOException e) {
            System.err.println("Error sending response: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            while (running) {
                HttpServerRequest request = new HttpServerRequest();
                String ln;
                while ((ln = reader.readLine()) != null) {
                    request.process(ln);
                    System.out.println(ln);
                    if (request.isDone()) {
                        send(out, Response.build(request));
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling request: " + e.getMessage());
        } finally {
            this.close();
        }
    }

    public void close() {
        try {
            running = false;
            socket.close();
        } catch (IOException e) {
            System.err.println("Error while closing session: " + e.getMessage());
        } finally {
            this.interrupt();
        }
    }

    public boolean isRunning() {
        return running;
    }

    private class Response {

        private static final LocalDateTime cachedTime = LocalDateTime.now();

        public static byte[] build(HttpServerRequest request) throws IllegalStateException {

            Map<String, String> headers = new HashMap<>();

            headers.put("Age", String.valueOf(ChronoUnit.SECONDS.between(cachedTime, LocalDateTime.now())));
            headers.put("Content-Type", "application/octet-stream");
            headers.put("Content-Encoding", "gzip");
            headers.put("Accept-Ranges", "bytes");
            headers.put("Cache-Control", "max-age=3600");
            headers.put("X-Content-Type-Options", "nosniff");
            headers.put("Server", "Not Telling");
            headers.put("Content-Length", "0");

            if (request == null) {
                throw new IllegalStateException("Request is null.");
            } else if (!request.isDone()) {
                throw new IllegalStateException("Request has not been fully processed.");
            } else {
                try {
                    Path filePath = Paths.get(request.getFile());
                    if (Files.exists(filePath)) {
                        String mimeType = getContentType(filePath.toString());
                        if (mimeType != null) {
                            headers.put("Content-Type", mimeType);
                            byte[] body = compress(Files.readAllBytes(filePath));

                            if (body != null) {
                                headers.put("Content-Length", String.valueOf(body.length));
                                return getFullResponse(Status.OK, headers, body);
                            } else {
                                return getStatusResponse(Status.NO_CONTENT);
                            }
                        } else {
                            throw new SecurityException("Invalid file type.");
                        }
                    } else {
                        throw new FileNotFoundException("File path was not found.");
                    }
                } catch (FileNotFoundException e) {
                    return getStatusResponse(Status.FILE_NOT_FOUND);
                } catch (SecurityException e) {
                    return getStatusResponse(Status.FORBIDDEN);
                } catch (IOException e) {
                    return getStatusResponse(Status.INTERNAL_SERVER_ERROR);
                } catch (Exception e) {
                    return getStatusResponse(Status.BAD_REQUEST);
                }
            }
        }

        public static byte[] compress(byte[] body) {

            if (body == null || body.length == 0) {
                return null;
            }
            try (
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)
                ) {
                gzipOutputStream.write(body);
                gzipOutputStream.finish();
                byte[] compressedData = byteArrayOutputStream.toByteArray();

                if (compressedData.length >= body.length) {
                    return null;
                }
                return compressedData;
            } catch (IOException e) {
                return null;
            }
        }

        private static byte[] getFullResponse(Status status, Map<String, String> headers, byte[] body) {

            StringBuilder response = new StringBuilder();
            response.append("HTTP/1.1 ");
            response.append(status.getCode());
            response.append("\r\n");

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                response.append(entry.getKey());
                response.append(": ");
                response.append(entry.getValue());
                response.append("\r\n");
            }

            byte[] responseBytes = response.toString().getBytes(StandardCharsets.UTF_8);
            byte[] fullResponse = new byte[responseBytes.length + body.length];

            System.arraycopy(responseBytes, 0, fullResponse, 0, responseBytes.length);
            System.arraycopy(body, 0, fullResponse, responseBytes.length, body.length);

            return fullResponse;
        }

        private static byte[] getStatusResponse(Status status) {
            StringBuilder response = new StringBuilder();
            response.append("HTTP/1.1 ");
            response.append(status.getCode());
            response.append("\r\n\r\n");

            return response.toString().getBytes(StandardCharsets.UTF_8);
        }

        private static String getContentType(String file) {

            int separatorIndex = file.indexOf(".");
            if (separatorIndex <= 0) {
                throw new IllegalArgumentException("Invalid file type.");
            }

            String extension = file.substring(separatorIndex + 1);
            ContentType contentType = ContentType.getFromExtension(extension);
            return contentType.getMimeType();
        }

        enum Status {
            OK("200 OK"),
            NO_CONTENT("204 No Content"),
            BAD_REQUEST("400 Bad Request"),
            FILE_NOT_FOUND("404 File Not Found"),
            FORBIDDEN("403 Forbidden"),
            INTERNAL_SERVER_ERROR("500 Internal Server Error");

            private final String code;

            Status(String code) {
                this.code = code;
            }

            public String getCode() {
                return code;
            }
        }

        enum ContentType {

            HTML("text/html; charset=UTF-8"),
            TXT("text/plain; charset=UTF-8"),
            JPEG("image/jpeg"),
            JPG("image/jpeg"),
            PNG("image/png"),
            GIF("image/gif");

            private final String mimeType;

            ContentType(String mimeType) {
                this.mimeType = mimeType;
            }

            public String getMimeType() {
                return mimeType;
            }

            public static ContentType getFromExtension(String extension) {
                for (ContentType type : ContentType.values()) {
                    if (extension.equalsIgnoreCase(type.name())) {
                        return type;
                    }
                }
                throw new IllegalArgumentException("The extension was not valid.");
            }
        }
    }
}
