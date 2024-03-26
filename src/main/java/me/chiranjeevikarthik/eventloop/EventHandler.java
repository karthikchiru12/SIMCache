package me.chiranjeevikarthik.eventloop;

import me.chiranjeevikarthik.resp.constants.EncodingConstants;
import me.chiranjeevikarthik.resp.constants.ParserConstants;
import me.chiranjeevikarthik.resp.parsing.Parser;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventHandler {

    private final Map<String, String> keyValueStore = new ConcurrentHashMap<>();

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
        int bytesRead;
        while ((bytesRead = clientChannel.read(buffer)) > 0) {
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
                    encounteredCRLF = false;
                }
            }
            buffer.clear();

            System.out.println(line);
            Parser parser = new Parser(line.toString());
            JSONArray parsedArray = parser.parse();
            System.out.println(parsedArray);
            for (int idx = 0; idx < parsedArray.length(); idx++) {
                List<JSONObject> parsedItemArray = (List<JSONObject>) parsedArray.getJSONObject(idx).get(ParserConstants.VALUE);
                JSONObject parsedItem = parsedItemArray.get(0);
                if (parsedItem.optString(ParserConstants.TYPE).equals(EncodingConstants.BULK_STRING_TYPE) &&
                        parsedItem.optString(ParserConstants.VALUE).equals("PING")) {
                    buffer.put("+PONG\r\n".getBytes());
                    System.out.println("+PONG");
                    buffer.flip();
                    clientChannel.write(buffer);
                } else {
                    buffer.put("*0\r\n".getBytes());
                    buffer.flip();
                    clientChannel.write(buffer);
                }
            }
        }
        if (bytesRead == -1) {
            System.out.println("Client Disconnected : " + clientChannel.getRemoteAddress());
            clientChannel.close();
            key.cancel();
        }
    }
}
