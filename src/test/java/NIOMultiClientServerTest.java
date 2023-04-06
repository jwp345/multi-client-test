import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
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
        Pipe pipe = Pipe.open();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Process server = startServer();
        InputStream inputStream = server.getInputStream();

        Thread.sleep(1000); // 서버가 실행될 때까지 약간의 텀이 필요함. 없으면 서버가 켜지기 전에 connection 시도로 connection refused exception 발생.

        for (int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    startClient(numClients.getAndDecrement(), pipe, inputStream); // 왜 DecrementAndGet()메소드는 일괄적인 값을 리턴할까..?
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        Thread.sleep(1000); // 마찬가지로 연결 종료시까지 시간이 필요함.

//        server.waitFor();
        server.destroy();
    }

    private void startClient(int num, Pipe pipe, InputStream inputStream) throws IOException {
        Selector selector = Selector.open();
        Pipe.SourceChannel sourceChannel = pipe.source();
        Pipe.SinkChannel sinkChannel = pipe.sink();
        sourceChannel.configureBlocking(false);
        sinkChannel.configureBlocking(false);
        sourceChannel.register(selector, SelectionKey.OP_READ); // 읽기 이벤트 등록
        sinkChannel.register(selector, SelectionKey.OP_WRITE); // 쓰기 이벤트 등록
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_CONNECT); // 연결 이벤트 등록
        socketChannel.connect(new InetSocketAddress("localhost", 1234));
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        loop : while(true) {
            selector.select();

            // 발생한 이벤트를 Set에 저장
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                if(key.isConnectable()) {
                    connectSocket(socketChannel, selector, num);
                }
                if(key.isReadable()) {
                    if(isEndReadByChannel(key, num, buffer)) {
                        break loop;
                    }
                }
                if(key.isWritable()) {
                    writeByChannel(key, inputStream, buffer);
                }
                keyIterator.remove();
            }
        }
//        sourceChannel.close();
//        sinkChannel.close();
    }

    private void connectSocket(SocketChannel socketChannel, Selector selector, int num) throws IOException {
        while (!socketChannel.finishConnect()) {}
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
//            socketChannel.register(selector, SelectionKey.OP_READ); // 결과는 같다.
            key.interestOps(SelectionKey.OP_READ);
        }
        if(key.channel() instanceof Pipe.SinkChannel) {
            Pipe.SinkChannel sinkChannel = (Pipe.SinkChannel) key.channel();
            int bytesRead = inputStream.read(buffer.array());
            if(bytesRead == -1) {
                return;
            } if(bytesRead > 0) {
                buffer.position(bytesRead); // 이게 뭐지?
                buffer.flip();
                sinkChannel.write(buffer);
                buffer.compact(); // 이것도 머냐?
                buffer.clear();
            }
        }
    }

    private boolean isEndReadByChannel(SelectionKey key, int num, ByteBuffer buffer) throws IOException {
        int bytesRead;
        if(key.channel() instanceof SocketChannel) {
            SocketChannel socketChannel = (SocketChannel) key.channel();
            bytesRead = socketChannel.read(buffer);
            String response = new String(buffer.array(), 0, bytesRead);
            System.out.println("Response : " + response);
            assertEquals("Hello, NIO Client!" + num, response.trim());
            socketChannel.close();
            return true; // 명시적으로 종료하기 위해 지금은 boolean 쓰지만 변경 예정
        }
        if(key.channel() instanceof Pipe.SourceChannel) {
            Pipe.SourceChannel sourceChannel = (Pipe.SourceChannel) key.channel();
            bytesRead = sourceChannel.read(buffer);

            if (bytesRead == -1) {
                return false;
            }
            if(bytesRead > 0) {
                buffer.flip();
                byte[] bytes = new byte[bytesRead];
                buffer.get(bytes);
                System.out.println(new String(bytes));
                buffer.clear();
            }
        }
        return false;
    }


}