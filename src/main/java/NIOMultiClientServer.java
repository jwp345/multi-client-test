import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NIOMultiClientServer {

    private Selector selector;
    private ByteBuffer buffer = ByteBuffer.allocate(1024);
    private ServerSocketChannel serverChannel;
    private int numClients = 0;

    public void stop() throws IOException {
        serverChannel.close();
        selector.close();
    }

    public void start() throws IOException {
        selector = Selector.open();

        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress("localhost", 1234));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server started");

        while (true) {
            int readyChannels = selector.select();
            if(readyChannels == 0) continue;

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();

                if (key.isAcceptable()) {
                    SocketChannel client = serverChannel.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ);
                    numClients++;
                    System.out.println("Client " + numClients + " connected.");
                } else if (key.isReadable()) {
                    SocketChannel client = (SocketChannel) key.channel();
                    buffer.clear();
                    int readBytes = client.read(buffer);
                    if (readBytes == -1) {
                        System.out.println("Client " + numClients + " disconnected.");
                        client.close();
                    } else {
                        buffer.flip();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        String message = new String(bytes);
                        System.out.println("Client " + numClients + " sent: " + message);
                    }
                }
                iter.remove();
            }
            System.out.println("thread count: " + ManagementFactory.getThreadMXBean().getThreadCount());
        }
    }

}