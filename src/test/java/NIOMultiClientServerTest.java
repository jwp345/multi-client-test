import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NIOMultiClientServerTest {

    private AtomicInteger numClients;
    private AtomicInteger endCnt;

    @BeforeEach
    public void setUp() {
        this.numClients = new AtomicInteger(9);
        this.endCnt = new AtomicInteger(9);
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
        InputStream inputStream = server.getInputStream();

        Thread.sleep(1000); // 서버가 실행될 때까지 약간의 텀이 필요함. 없으면 서버가 켜지기 전에 connection 시도로 connection refused exception 발생.
        Pipe pipe = Pipe.open();


        for (int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    startClient(numClients.getAndDecrement(), inputStream, pipe); // 왜 DecrementAndGet()메소드는 일괄적인 값을 리턴할까..?
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//        server.waitFor();
        server.destroy();
    }

    private void startClient(int num, InputStream inputStream, Pipe pipe) throws IOException {
        Selector selector = Selector.open();
        Pipe.SourceChannel sourceChannel = pipe.source();
        Pipe.SinkChannel sinkChannel = pipe.sink();
        sourceChannel.configureBlocking(false);
        sinkChannel.configureBlocking(false);
        sourceChannel.register(selector, SelectionKey.OP_READ); // Pipe 읽기 이벤트 등록
        sinkChannel.register(selector, SelectionKey.OP_WRITE); // Pipe 쓰기 이벤트 등록
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_CONNECT); // 연결 이벤트 등록
        socketChannel.connect(new InetSocketAddress("localhost", 1234));
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        while(true) {
            selector.select();

            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                System.out.println(key.channel());

                if(key.isValid() && key.isConnectable()) {
                    connectSocket(socketChannel, selector, num);
                }
                if(key.isValid() && key.isReadable()) {
                    readByChannel(key, num, buffer, sinkChannel);
                }
                if(key.isValid() && key.isWritable()) { // readable & writable 되는 소켓이 있는듯..?
                    writeByChannel(key, inputStream, buffer);
                }
                keyIterator.remove();
            }
        }
    }

    private void connectSocket(SocketChannel socketChannel, Selector selector, int num) throws IOException {
        while (!socketChannel.finishConnect()) {} // 연결이 완료되었는지 검사
        ByteBuffer writeBuffer = ByteBuffer.allocate(1024);
        String message = "Hello, NIO Server!" + num;
        writeBuffer.put(message.getBytes());
        writeBuffer.flip();
        socketChannel.register(selector, SelectionKey.OP_WRITE, writeBuffer);
    }

    private void writeByChannel(SelectionKey key, InputStream inputStream, ByteBuffer buffer) throws IOException {
        if(key.channel() instanceof SocketChannel) {
            SocketChannel socketChannel = (SocketChannel) key.channel();
            ByteBuffer writeBuffer = (ByteBuffer) key.attachment();
            socketChannel.write(writeBuffer);
            key.interestOps(SelectionKey.OP_READ); // 한번만 쓰고 읽기로 전환
            writeBuffer.clear();
        }
        if(key.channel() instanceof Pipe.SinkChannel) {
            Pipe.SinkChannel sinkChannel = (Pipe.SinkChannel) key.channel();
            int bytesRead = inputStream.read(buffer.array());
            if(bytesRead <= 0) { // 원인 찾았다 얘가 계속 읽어오고 있음..
                key.interestOps(0);
                key.cancel();
                System.out.println("running..");
                return;
            }
            buffer.position(bytesRead); // 버퍼의 위치 설정
            buffer.flip();
            sinkChannel.write(buffer);
//            buffer.compact(); // 뭐하는 건지 잘 모르겠음.
            buffer.clear();
        }
    }

    private void readByChannel(SelectionKey key, int num, ByteBuffer buffer, Pipe.SinkChannel sinkChannel) throws IOException {
        int bytesRead;
        if(key.channel() instanceof SocketChannel) {
            SocketChannel socketChannel = (SocketChannel) key.channel();
            bytesRead = socketChannel.read(buffer);

            String response = new String(buffer.array(), 0, bytesRead);
            System.out.println("Response : " + response);
            assertEquals("Hello, NIO Client!" + num, response.trim());
            buffer.clear();
//            socketChannel.socket().setSoLinger(true, 1);
            socketChannel.close(); // active close
        }
        if(key.channel() instanceof Pipe.SourceChannel) {
            Pipe.SourceChannel sourceChannel = (Pipe.SourceChannel) key.channel();
            bytesRead = sourceChannel.read(buffer);

            if (bytesRead <= 0) {
                key.interestOps(0);
                key.cancel();
                System.out.println("read running.." + bytesRead);
                return;
            }
            buffer.clear();
            buffer.flip();
            String response = new String(buffer.array(), 0, bytesRead);
            System.out.println(response);
            String endMsg = "Client disconnected";
            if(response.trim().contains(endMsg)) {
                System.out.println("reach event");
                key.cancel();
                sourceChannel.close();
                sinkChannel.close();
                key.interestOps(0);
            }
//            buffer.clear();
        }
    }
}