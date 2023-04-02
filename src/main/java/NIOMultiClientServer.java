import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NIOMultiClientServer implements Runnable{

    private Selector selector;
    private ByteBuffer buffer = ByteBuffer.allocate(1024); // byteBuffer 크기를 꼭 맞춰줘야 할것 같다 나머지는?
    private ServerSocketChannel serverChannel;
    private int numClients = 0;

    @Override
    public void run() {

        try {
            selector = Selector.open();

            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress("localhost", 1234));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                int readyChannels = selector.select();
                System.out.println("event amount : " + readyChannels);
                if(readyChannels == 0) continue;

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if(key.isAcceptable()) {
                        SocketChannel client = serverChannel.accept();
                        System.out.println("Server start");
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                    }
                    if (key.isReadable()) { // selectionKey가 read 일 경우
                        SocketChannel client = (SocketChannel) key.channel();
                        int readBytes = client.read(buffer);
                        if (readBytes == -1) {
                            System.out.println("Client " + numClients + " disconnected.");
                            client.close();
                        } else {
                            buffer.flip();
                            byte[] bytes = new byte[buffer.remaining()];
                            buffer.get(bytes);
                            String message = new String(bytes);
                            buffer.clear();
                            buffer.put(bytes);
                            client.register(selector, SelectionKey.OP_WRITE);
                            System.out.println("Client " + numClients + " sent: " + message);
                        }
                    } if(key.isWritable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        client.write(buffer);
                        buffer.clear();
                    }
                    iter.remove();
                    System.out.println("thread count: " + ManagementFactory.getThreadMXBean().getThreadCount());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}