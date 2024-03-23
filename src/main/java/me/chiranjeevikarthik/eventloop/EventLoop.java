package me.chiranjeevikarthik.eventloop;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

public class EventLoop {
    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;
    private final EventHandler eventHandler;
    private volatile boolean isRunning;
    private final long maxTimeoutMillis;

    public EventLoop(int port, EventHandler eventHandler, long maxTimeoutMillis) throws IOException {
        this.selector = Selector.open();
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.socket().bind(new InetSocketAddress(port));
        this.serverSocketChannel.configureBlocking(false);
        this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.eventHandler = eventHandler;
        this.isRunning = true;
        this.maxTimeoutMillis = maxTimeoutMillis;
    }

    public void start() {
        while (isRunning) {
            try {
                selector.select(maxTimeoutMillis);

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        eventHandler.handleAccept(key);
                    }

                    if (key.isReadable()) {
                        try {
                            eventHandler.handleRead(key);
                        } catch (IOException e) {
                            System.err.println("Error handling read event: " + e.getMessage());
                            key.cancel();
                            key.channel().close();
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Event loop error: " + e.getMessage());
            }
        }
        closeResources();
    }

    public void stop() {
        isRunning = false;
        selector.wakeup();
    }

    private void closeResources() {
        try {
            serverSocketChannel.close();
            selector.close();
        } catch (IOException e) {
            System.err.println("Error closing server socket channel or selector: " + e.getMessage());
        }
    }
}
