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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NIOMultiClientServerTest {

    private int numClients;
    private NIOMultiClientServer server;

    private List<SocketChannel> usedSockets;

    @BeforeEach
    public void setUp() {
        this.numClients = 10;
        this.usedSockets = new ArrayList<>();
        this.server = new NIOMultiClientServer();
        new Thread(server).start();
    }

    @Test
    public void testNIOClient() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        while(numClients-- > 0) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    startClient();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        /*usedSockets.stream().forEach(channel -> {
            try {
                channel.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        */
    }

    private void startClient() throws IOException{
        SocketChannel socketChannel = SocketChannel.open();
        usedSockets.add(socketChannel);
//        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress("localhost", 1234));

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        String message = "Hello, NIO Server!";
        buffer.put(message.getBytes());
        buffer.flip();
        socketChannel.write(buffer);

        buffer.clear();
        int bytesRead = socketChannel.read(buffer);
        String response = new String(buffer.array(), 0, bytesRead);
        System.out.println("Response : " + response);
        assertEquals("Hello, NIO Client!", response.trim());
        socketChannel.close();
//        socketChannel.shutdownInput();
//        socketChannel.shutdownOutput();
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