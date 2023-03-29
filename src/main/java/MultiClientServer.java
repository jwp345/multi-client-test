import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MultiClientServer {

    private ServerSocket serverSocket;

    public MultiClientServer() {
        try {
            this.serverSocket = new ServerSocket(1234);
            Socket clientSocket = serverSocket.accept();
            Thread clientThread = new Thread(new ClientHandler(clientSocket));
            clientThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() throws IOException {
        serverSocket.close();
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
