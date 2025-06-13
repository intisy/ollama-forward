package io.github.intisy.ollama.forward;

import io.github.intisy.simple.logger.LogLevel;

/**
 * @author Finn Birich
 */
public class TestServer {
    public static void main(String[] args) {
        Server server = new Server();
        server.getLogger().setLogLevel(LogLevel.DEBUG);
        server.setTest(true);
        server.start();
    }
}
