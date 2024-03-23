package me.chiranjeevikarthik.eventloop;

import me.chiranjeevikarthik.resp.parsing.Parser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class EventHandler {

    public EventHandler() {
    }

    public void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            clientChannel.register(key.selector(), SelectionKey.OP_READ);
            System.out.println("Accepted new connection from: " + clientChannel.getRemoteAddress());
        }
    }

    public void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();

        //https://stackoverflow.com/a/15950481
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        StringBuffer line = new StringBuffer();
        int bytesRead = clientChannel.read(buffer);
        if (bytesRead == -1) {
            clientChannel.close();
            key.cancel();
            return;
        }

        buffer.flip();
        boolean encounteredCRLF = false;
        while (buffer.hasRemaining()) {
            char c = (char) buffer.get(); // Read character from buffer
            if (c == '\\') {
                encounteredCRLF = true;
            } else if (c == 'r') {
                if (encounteredCRLF) line.append("\r");
                encounteredCRLF = false;
            } else if (c == 'n') {
                if (encounteredCRLF) line.append("\n");
                encounteredCRLF = false;
            } else {
                // Append character to line
                line.append(c);
            }
        }
        buffer.clear();

        System.out.println(line);
        Parser parser = new Parser(line.toString());
        System.out.println(parser.parse());
        if(line.toString().contains("PING"))
        {
            buffer.put("+PONG".getBytes());
            buffer.flip();
            clientChannel.write(buffer);
        }

    }
}
