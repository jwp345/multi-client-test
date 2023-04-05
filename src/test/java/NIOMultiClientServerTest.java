import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NIOMultiClientServerTest {

    private int numClients;
    Process server;

    @BeforeEach
    public void setUp() {
        this.numClients = 10;
//        server = startServer();
    }

    private Process startServer() throws IOException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = NIOMultiClientServer.class.getCanonicalName();

        ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", classpath, className);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        return builder.start();
    }

    @Test
    public void testSocketChannelClient() {
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
//        server.destroy();
        /*usedSockets.stream().forEach(channel -> {
            try {
                channel.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }); // 비동기 코드 연습
        */
    }

    private void startClient() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
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

//    @AfterEach
//    void cleanUp() {
//        server.destroy();
//
//    }

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