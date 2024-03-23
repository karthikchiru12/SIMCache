package me.chiranjeevikarthik;

import me.chiranjeevikarthik.eventloop.EventHandler;
import me.chiranjeevikarthik.eventloop.EventLoop;

import java.io.IOException;

public class SIMCache {

    public static void main(String[] args) {
        EventHandler eventHandler = new EventHandler();
        long maxTimeoutMillis = 1000;
        try {
            EventLoop eventLoop = new EventLoop(8080, eventHandler, maxTimeoutMillis);
            eventLoop.start();
        } catch (IOException e) {
            System.err.println("Error creating EventLoop: " + e.getMessage());
        }
    }
}
