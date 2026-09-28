import java.util.HashMap;
import java.util.Map;

public class HttpServerRequest {

    private int n = 0;
    private boolean done = false;
    private boolean valid = false;

    private String method;
    private String version;
    private String file;
    private String host;

    private Map<String, String> headers = new HashMap<>();

    public boolean isDone() {
        return done;
    }

    public String getMethod() {
        return method;
    }

    public String getFile() {
        return file;
    }

    public String getVersion() {
        return version;
    }

    public String getHost() {
        return headers.get("HOST");
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void process(String ln) {

        try {
            if (ln == null) {
                throw new IllegalArgumentException("Null line was given to process.");
            } else if (done) {
                throw new IllegalStateException("This HttpServerRequest has already been processed.");
            } else if (ln.trim().isEmpty()) {
                done = true;
            } else if (n == 0) {
                n++;
                parseRequestLn(ln);
            } else {
                n++;
                parseHeaderLn(ln);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Exception caught on line " + n + " of request: " + e.getMessage());
        }
    }

    private void parseHeaderLn(String ln) {

        String[] parts = ln.split(":", 2);
        if (parts.length != 2) {
            System.out.println("Unexpected header format in request line.");
            return;
        } else {
            headers.put(parts[0].trim().toUpperCase(), parts[1].trim());
        }
    }

    private void parseRequestLn(String ln) {

        String[] parts = ln.split(" ");
        if (parts.length != 3) {
            System.out.println("Unexpected length of request line.");
            return;           
        } else {
            method = parts[0].trim().toUpperCase();
            file = parts[1].trim().toLowerCase();
            version = parts[2].trim().toUpperCase();
            if (file.startsWith("/")) {
                file = file.substring(1);
            }
        }
    }
}
