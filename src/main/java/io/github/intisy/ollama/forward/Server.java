package io.github.intisy.ollama.forward;

import com.sun.net.httpserver.HttpServer;
import io.github.intisy.ollama.forward.proxy.handler.ChatHandler;
import io.github.intisy.ollama.forward.proxy.handler.ForwardingHandler;
import io.github.intisy.ollama.forward.proxy.handler.GenerateHandler;
import io.github.intisy.ollama.forward.proxy.handler.TagsHandler;
import io.github.intisy.ollama.forward.settings.SecureStore;
import io.github.intisy.ollama.forward.settings.Settings;
import io.github.intisy.simple.logger.LogLevel;
import io.github.intisy.simple.logger.SimpleLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * @author Finn Birich
 */
public class Server {
    private static final int PORT = 11435;
    private static final int MAX_RETRIES = 12;
    private static final int RETRY_DELAY_SECONDS = 5;

    private final SimpleLogger logger;
    private final Settings settings;
    private final SecureStore secureStore;
    private boolean test;
    private HttpServer server;

    public Server() {
        this.logger = new SimpleLogger();
        this.settings = new Settings();
        this.secureStore = new SecureStore();
    }

    public SecureStore getSecureStore() {
        return secureStore;
    }

    public Settings getSettings() {
        return settings;
    }

    public SimpleLogger getLogger() {
        return logger;
    }

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
        this.settings.setTest(test);
        this.secureStore.setTest(test);
    }

    public void start() {
        logger.debug("Starting proxy server...");

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                this.server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);
                break;
            } catch (BindException bindException) {
                logger.error("Proxy server port " + PORT + " is already in use.", bindException);
                if (attempt < MAX_RETRIES) {
                    logger.error("Retrying in " + RETRY_DELAY_SECONDS + " seconds... (Attempt " + attempt + "/" + MAX_RETRIES + ")");
                    try {
                        Thread.sleep(RETRY_DELAY_SECONDS * 1000);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        logger.error("Proxy server startup was interrupted. Aborting.", interruptedException);
                        return;
                    }
                } else {
                    logger.error("Could not bind to port " + PORT + " after " + MAX_RETRIES + " attempts. The server will not start.", bindException);
                }
            } catch (IOException ioException) {
                logger.error("An unexpected I/O error occurred while starting the proxy server.", ioException);
                return;
            }
        }

        server.createContext("/api/tags", new TagsHandler(this));
        server.createContext("/api/chat", new ChatHandler(this));
        server.createContext("/api/generate", new GenerateHandler(this));
        server.createContext("/api/version", new ForwardingHandler(this, "/api/version"));
        server.createContext("/api/show", new ForwardingHandler(this, "/api/show"));
        server.createContext("/api/create", new ForwardingHandler(this, "/api/create"));
        server.createContext("/api/copy", new ForwardingHandler(this, "/api/copy"));
        server.createContext("/api/delete", new ForwardingHandler(this, "/api/delete"));
        server.createContext("/api/pull", new ForwardingHandler(this, "/api/pull"));
        server.createContext("/api/push", new ForwardingHandler(this, "/api/push"));
        server.createContext("/api/embeddings", new ForwardingHandler(this, "/api/embeddings"));
        server.createContext("/api/ps", new ForwardingHandler(this, "/api/ps"));
        server.createContext("/api/blobs", new ForwardingHandler(this, "/api/blobs"));

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            logger.debug("Received request for unknown path: " + path);
            String msg = "Ollama is running";
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, msg.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(msg.getBytes(StandardCharsets.UTF_8));
            }
        });

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        logger.debug("Ollama forward proxy server started successfully on http://localhost:" + PORT);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    private void stop() {
        if (server != null) {
            logger.info("Stopping Ollama forward proxy server...");
            server.stop(1);
            logger.info("Proxy server stopped.");
        }
    }
}
