import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class HttpServer {

    static final int PN = 5000;

    public static void main(String[] args) {
        try {
            String address = String.format("http://%s:", InetAddress.getLocalHost().getHostName());
            int port = PN;

            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            }

            System.out.println("Web server starting on...");
            System.out.println(address + port);

            ServerSocket ss = new ServerSocket(port);
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            ss.setSoTimeout(15000);

            try {
                Socket socket = ss.accept();
                System.out.println("New connection from: " + socket.getRemoteSocketAddress());

                HttpServerSession session = new HttpServerSession(socket);
                session.start();

                while (session.isRunning()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new IOException("Session ended.");
                    }
                }

            } catch (IOException e) {
                System.err.println("Error while connected: " + e.getMessage());
            } finally {
                reader.close();
                ss.close();
            }

        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error while waiting: " + e.getMessage());
        } finally {
            System.out.println("Web server closing...");
        }
    }
}
