import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MultiClientServer {
    public MultiClientServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(10000);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("클라이언트 연결 완료");
                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
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
                    System.out.println("Server received: " + inputLine);
                    out.println(inputLine.toUpperCase());
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
}
