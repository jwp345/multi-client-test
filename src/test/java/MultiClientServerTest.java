import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;


public class MultiClientServerTest {
    @Test
    public void testMultiClientServer() throws Exception {
        // Start server
        MultiClientServer server = new MultiClientServer();
        server.start();

        // Connect clients
        Socket[] clients = new Socket[10];
        for (int i = 0; i < 10; i++) {
            clients[i] = new Socket("localhost", 1234);
        }

        // Send messages from clients
        PrintWriter[] outs = new PrintWriter[10];
        for (int i = 0; i < 10; i++) {
            outs[i] = new PrintWriter(clients[i].getOutputStream(), true);
            outs[i].println("Client " + i + " message");
        }

        // Receive messages from server
        BufferedReader[] ins = new BufferedReader[10];
        for (int i = 0; i < 10; i++) {
            ins[i] = new BufferedReader(new InputStreamReader(clients[i].getInputStream()));
            assertEquals("Server received: Client " + i + " message", ins[i].readLine());
        }

        // Close connections
        for (int i = 0; i < 10; i++) {
            clients[i].close();
        }
        server.stop();
    }
}