import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NIOMultiClientServerTest {
    private NIOMultiClientServer server;
    private Selector selector;
    private SocketChannel clientChannel1, clientChannel2, clientChannel3, clientChannel4, clientChannel5,
            clientChannel6, clientChannel7, clientChannel8, clientChannel9, clientChannel10;

    @BeforeEach
    public void setup() throws IOException {
        server = new NIOMultiClientServer();
//        server.start();

        selector = Selector.open();

        clientChannel1 = SocketChannel.open(new InetSocketAddress("localhost", 1234));
        clientChannel1.configureBlocking(false);
        clientChannel1.register(selector, SelectionKey.OP_WRITE);

        clientChannel2 = SocketChannel.open(new InetSocketAddress("localhost", 1234));
        clientChannel2.configureBlocking(false);
        clientChannel2.register(selector, SelectionKey.OP_WRITE);

        clientChannel3 = SocketChannel.open(new InetSocketAddress("localhost", 1234));
        clientChannel3.configureBlocking(false);
        clientChannel3.register(selector, SelectionKey.OP_WRITE);

        clientChannel4 = SocketChannel.open(new InetSocketAddress("localhost", 1234));
        clientChannel4.configureBlocking(false);
        clientChannel4.register(selector, SelectionKey.OP_WRITE);

        clientChannel5 = SocketChannel.open(new InetSocketAddress("localhost", 1234));
        clientChannel5.configureBlocking(false);
        clientChannel5.register(selector, SelectionKey.OP_WRITE);

        clientChannel6 = SocketChannel.open(new InetSocketAddress("localhost", 1234));
        clientChannel6.configureBlocking(false);
        clientChannel6.register(selector, SelectionKey.OP_WRITE);

        clientChannel7 = SocketChannel.open(new InetSocketAddress("localhost", 1234));
        clientChannel7.configureBlocking(false);
        clientChannel7.register(selector, SelectionKey.OP_WRITE);

        clientChannel8 = SocketChannel.open(new InetSocketAddress("localhost", 1234));
        clientChannel8.configureBlocking(false);
        clientChannel8.register(selector, SelectionKey.OP_WRITE);

        clientChannel9 = SocketChannel.open(new InetSocketAddress("localhost", 1234));
        clientChannel9.configureBlocking(false);
        clientChannel9.register(selector, SelectionKey.OP_WRITE);

        clientChannel10 = SocketChannel.open(new InetSocketAddress("localhost", 1234));
        clientChannel10.configureBlocking(false);
        clientChannel10.register(selector, SelectionKey.OP_WRITE);
    }

    @AfterEach
    public void cleanup() throws IOException {
        clientChannel1.close();
        clientChannel2.close();
        clientChannel3.close();
        clientChannel4.close();
        clientChannel5.close();
        clientChannel6.close();
        clientChannel7.close();
        clientChannel8.close();
        clientChannel9.close();
        clientChannel10.close();

        server.stop();
    }

    @Test
    public void testMultiClientServer() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        clientChannel1.write(ByteBuffer.wrap("Client 1 message".getBytes()));
        clientChannel2.write(ByteBuffer.wrap("Client 2 message".getBytes()));
        clientChannel3.write(ByteBuffer.wrap("Client 3 message".getBytes()));
        clientChannel4.write(ByteBuffer.wrap("Client 4 message".getBytes()));
        clientChannel5.write(ByteBuffer.wrap("Client 5 message".getBytes()));
        clientChannel6.write(ByteBuffer.wrap("Client 6 message".getBytes()));
        clientChannel7.write(ByteBuffer.wrap("Client 7 message".getBytes()));
        clientChannel8.write(ByteBuffer.wrap("Client 8 message".getBytes()));
        clientChannel9.write(ByteBuffer.wrap("Client 9 message".getBytes()));
        clientChannel10.write(ByteBuffer.wrap("Client 10 message".getBytes()));

        int readyChannels = selector.select();
        assertTrue(readyChannels > 0);

        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

        while (keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();

            if (key.isWritable()) {
                SocketChannel clientChannel = (SocketChannel) key.channel();
                buffer.clear();
                buffer.put("Server message".getBytes());
                buffer.flip();
                clientChannel.write(buffer);
                clientChannel.register(selector, SelectionKey.OP_READ);
            } else if (key.isReadable()) {
                SocketChannel clientChannel = (SocketChannel) key.channel();
                buffer.clear();
                int numBytes = clientChannel.read(buffer);
                String receivedMessage = new String(buffer.array(), 0, numBytes);
                assertEquals("Server message", receivedMessage.trim());
                clientChannel.close();
            }

            keyIterator.remove();
        }
    }
}