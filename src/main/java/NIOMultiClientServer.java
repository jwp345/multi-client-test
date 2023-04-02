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

    @Override
    public void run() {

        try {
            selector = Selector.open();

            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress("localhost", 1234));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Server start");

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
                        client.configureBlocking(false);


                        while (client.read(buffer) > -1) {
                            buffer.flip();
                            byte[] bytes = new byte[buffer.remaining()];
                            buffer.get(bytes);
                            String message = new String(bytes);
                            buffer.clear();
                            buffer.put(bytes);
                            System.out.println("Client sent: " + message);
                            buffer.flip();
                            client.write(buffer);
                            buffer.clear();
                        }
                        client.close();
                        System.out.println("Client disconnected.");
                    }
                    System.out.println("thread count: " + ManagementFactory.getThreadMXBean().getThreadCount());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}