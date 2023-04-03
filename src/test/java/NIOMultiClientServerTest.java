import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NIOMultiClientServerTest {

    private int numClients;

    @BeforeEach
    public void setUp() throws IOException {
        this.numClients = 10;
        NIOMultiClientServer server = new NIOMultiClientServer();
        new Thread(server).start();
    }

    @Test
    public void testNIOClient() throws IOException, InterruptedException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress("localhost", 1234));
        while (!socketChannel.finishConnect()) {
            // 연결이 완료되기를 기다립니다.
        }
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        String message = "Hello, NIO Server!";
        buffer.put(message.getBytes());
        buffer.flip();
        while (buffer.hasRemaining()) {
            socketChannel.write(buffer);
        }
        buffer.clear();
        Thread.sleep(1000);
        int bytesRead = socketChannel.read(buffer);
        String response = new String(buffer.array(), 0, bytesRead);
        System.out.println("Response : " + response);
        assertEquals("Hello, NIO Client!", response.trim());
        socketChannel.close();
    }

    /*
    @Test
    @DisplayName("socketChannel까지만 이용한 테스트")
    void testMultipleClients() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(numClients);
        for (int i = 0; i < numClients; i++) {

            Thread clientThread = new Thread(() -> {
                try {
                    SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 1234));
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    buffer.put("hello server".getBytes());
                    buffer.flip(); // 읽기 -> 쓰기로 변경하거나 그반대
                    socketChannel.write(buffer);
                    buffer.clear();
                    socketChannel.read(buffer);
                    buffer.flip();
                    String response = new String(buffer.array()).trim();
                    assertEquals("Server received: hello server", response);
                    socketChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
            clientThread.start();
        }
        Thread.sleep(1000);

        // wait for all clients to finish
        latch.await();
    }


 */
}