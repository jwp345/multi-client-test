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
    public void setUp() {
        this.numClients = 10;
        NIOMultiClientServer server = new NIOMultiClientServer();
        new Thread(server).start();
    }

    @Test
    @DisplayName("클라이언트에서도 selector와 nio소켓을 이용해 통신")
    public void testSelectorClients() {
        for(int i = 0; i < numClients; i++) {
            int clientId = i;
            new Thread(() -> {
                try {
                    SocketChannel channel = SocketChannel.open();
                    channel.configureBlocking(false);
                    channel.connect(new InetSocketAddress("localhost", 1234));

                    Selector selector = Selector.open();
                    channel.register(selector, SelectionKey.OP_CONNECT);

                    while (true) {
                        int readyChannels = selector.select();
                        if (readyChannels == 0) {
                            continue;
                        }

                        Set<SelectionKey> selectedKeys = selector.selectedKeys();
                        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                        while (keyIterator.hasNext()) {
                            SelectionKey key = keyIterator.next();

                            if (key.isConnectable()) {
                                SocketChannel client = (SocketChannel) key.channel();
                                if (client.isConnectionPending()) {
                                    client.finishConnect();
                                    System.out.println("Client " + clientId + " connected.");
                                    ByteBuffer buffer = ByteBuffer.wrap(("Hello from client " + clientId).getBytes());
                                    client.register(selector, SelectionKey.OP_WRITE, buffer);
                                }
                            } else if (key.isWritable()) {
                                SocketChannel client = (SocketChannel) key.channel();
                                ByteBuffer buffer = (ByteBuffer) key.attachment();
                                buffer.rewind();
                                client.write(buffer);
                                client.register(selector, SelectionKey.OP_READ);
                            } else if (key.isReadable()) {
                                SocketChannel client = (SocketChannel) key.channel();
                                ByteBuffer buffer = ByteBuffer.allocate(1024);

                                while (client.read(buffer) > -1) {
                                    buffer.flip();
                                    byte[] bytes = new byte[buffer.remaining()];
                                    buffer.get(bytes);
                                    String message = new String(bytes);
                                    System.out.println("Client " + clientId + " received: " + message);
                                    client.register(selector, SelectionKey.OP_WRITE, ByteBuffer.wrap(("Hello from client " + clientId).getBytes()));
                                }
                                client.close();
                            }
                            keyIterator.remove();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

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