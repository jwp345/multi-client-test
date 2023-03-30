import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;

public class MultiClientServer implements Runnable {

    private ServerSocket serverSocket;
    private boolean running;

    public MultiClientServer() {
        try {
            this.serverSocket = new ServerSocket(1234);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        running = true;
        new Thread(this).start();
    }

    public void stop() throws IOException {
        running = false;
        serverSocket.close();
    }

    @Override
    public void run() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
                System.out.println("thread count: " + ManagementFactory.getThreadMXBean().getThreadCount());
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }

    class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) throws IOException {
            this.clientSocket = socket;
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
//                    System.out.println("Server received: " + inputLine);
                    out.println("Server received: " + inputLine);
//                    out.flush();
                }
                clientSocket.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
