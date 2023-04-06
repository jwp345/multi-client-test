import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NIOMultiClientServerTest {

    private AtomicInteger numClients;

    @BeforeEach
    public void setUp() {
        this.numClients = new AtomicInteger(9);
    }

    private Process startServer() throws IOException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = NIOMultiClientServer.class.getCanonicalName();

        ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", classpath, className);
//        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT); // 기본 값 PIPE
        return builder.start();
    }

    @Test
    public void testSocketChannelClient() throws IOException, InterruptedException {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Process server = startServer();
        Thread.sleep(1000); // 서버가 실행될 때까지 약간의 텀이 필요함. 없으면 서버가 켜지기 전에 connection 시도로 connection refused exception 발생.

        for(int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    startClient(numClients.getAndDecrement()); // 왜 DecrementAndGet()메소드는 일괄적인 값을 리턴할까..?
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        server.destroy();
//        server.waitFor();
        copy(server.getInputStream(), System.out);
    }

    private void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int n = 0;
        while ((n = input.read(buffer)) != -1) {
            output.write(buffer, 0, n);
        }
    }

    private void startClient(int num) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
//        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress("localhost", 1234));

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        String message = "Hello, NIO Server!" + num;
        buffer.put(message.getBytes());
        buffer.flip();
        socketChannel.write(buffer);

        buffer.clear();
        int bytesRead = socketChannel.read(buffer);
        String response = new String(buffer.array(), 0, bytesRead);
        System.out.println("Response : " + response);
        assertEquals("Hello, NIO Client!" + num, response.trim());
        socketChannel.close();
//        socketChannel.shutdownInput();
//        socketChannel.shutdownOutput();
    }

//    @AfterEach
//    void cleanUp() {
//        server.destroy();
//
//    }

}