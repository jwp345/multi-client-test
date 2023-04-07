import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NIOMultiClientServer {

    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().setReuseAddress(true);
        serverSocketChannel.bind(new InetSocketAddress("localhost", 1234));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");

        // 무한루프를 돌며 Selector에서 발생한 이벤트 처리
        while (true) {
            // Selector에서 발생한 이벤트를 기다림 (Blocking method)
            selector.select();

            // 발생한 이벤트를 Set에 저장
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                if (key.isAcceptable()) {
                    acceptConnection(key, selector);
                }
                if (key.isReadable()) {
                    readMessage(key, selector, serverSocketChannel);
                }
                if (key.isWritable()) {
                    writeMessage(key, selector);
                }

                keyIterator.remove();
            }
            System.out.println("Active threads : " + Thread.activeCount());
            /*
            Map<Thread, StackTraceElement[]> stackTraceMap = Thread.getAllStackTraces();
            for (Thread t : stackTraceMap.keySet()) {
                System.out.println("Thread name: " + t.getName());
            }

             */
        }
    }

    private static void acceptConnection(SelectionKey key, Selector selector) throws IOException {
        // ServerSocketChannel에서 SocketChannel을 생성하여 연결을 수락함
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();

        // SocketChannel을 Non-blocking 모드로 설정하고 Selector에 등록
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);

        // 연결된 클라이언트 정보 출력
        System.out.println("Accepted new connection from client: " + socketChannel.getRemoteAddress());
    }

    private static void readMessage(SelectionKey key, Selector selector, ServerSocketChannel serverSocketChannel) throws IOException {
        // SelectionKey에서 SocketChannel 객체를 가져옴
        SocketChannel socketChannel = (SocketChannel) key.channel();

        // 클라이언트로부터 데이터를 읽어옴
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int readBytes = socketChannel.read(buffer);

        // 클라이언트가 연결을 끊은 경우 or 스트림에서 더이상 읽을 값이 없을 경우(end of stream)
        if (readBytes == -1) {
//            key.interestOps(0); // 관심 키 제거
            System.out.println("Client disconnected");
//            socketChannel.socket().setSoLinger(true, 0);
            serverSocketChannel.close();
            return;
        }

        // 읽어온 데이터를 String 객체로 변환
        String message = new String(buffer.array(), 0, readBytes).trim();

        // 클라이언트로부터 전송받은 데이터 출력
        System.out.println("Received message from client: " + message);

        // 변환된 데이터를 ByteBuffer 객체로 변환하여 SocketChannel에 기록함
        ByteBuffer writeBuffer = ByteBuffer.allocate(1024);
        writeBuffer.put(("Hello, NIO Client!" + message.charAt(message.length() - 1)).getBytes());
        writeBuffer.flip();

        socketChannel.register(selector, SelectionKey.OP_WRITE, writeBuffer); // ByteBuffer로 기록해야 이벤트 발생?

        System.out.println("Registered Write event for client: " + socketChannel.getRemoteAddress());
    }

    private static void writeMessage(SelectionKey key, Selector selector) throws IOException {
        // SelectionKey에서 SocketChannel과 ByteBuffer 객체를 가져옴
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer writeBuffer = (ByteBuffer) key.attachment();
        // ByteBuffer에 있는 데이터를 SocketChannel에 기록함
        socketChannel.write(writeBuffer);
//        System.out.println("write message : " + new String(writeBuffer.array(), 0, 1024).trim());
        // ByteBuffer의 데이터를 모두 기록한 경우

        if (!writeBuffer.hasRemaining()) {
//            key.interestOps(SelectionKey.OP_READ); // 다음 읽기 이벤트를 수신하기 위해 읽기 모드로 변경
            socketChannel.register(selector, SelectionKey.OP_READ);
//            System.out.println("Response sent to client: " + socketChannel.getRemoteAddress());
        }
    }

}