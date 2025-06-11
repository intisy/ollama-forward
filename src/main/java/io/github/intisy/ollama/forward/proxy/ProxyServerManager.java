package io.github.intisy.ollama.forward.proxy;

import com.esotericsoftware.kryo.kryo5.minlog.Log;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.sun.net.httpserver.HttpServer;
import io.github.intisy.ollama.forward.settings.PluginSettingsService;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;

public class ProxyServerManager implements ProjectActivity {
    private static final int PORT = 11435;
    private static final int MAX_RETRIES = 12;
    private static final int RETRY_DELAY_SECONDS = 5;

    private static HttpServer server;

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        synchronized (ProxyServerManager.class) {
            if (server == null) {
                start();
            }
        }
        return Unit.INSTANCE;
    }

    private void start() {
        Log.debug("[DEBUG] Starting proxy server...");
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);
                server.createContext("/api/tags", new TagsHandler());
                server.createContext("/api/chat", new ChatHandler());
                server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
                server.start();

                Log.debug("Ollama forward proxy server started successfully on http://localhost:" + PORT);
                Runtime.getRuntime().addShutdownHook(new Thread(ProxyServerManager::stop));

                return;

            } catch (BindException bindException) {
                Log.error("Proxy server port " + PORT + " is already in use.", bindException);
                if (attempt < MAX_RETRIES) {
                    Log.error("Retrying in " + RETRY_DELAY_SECONDS + " seconds... (Attempt " + attempt + "/" + MAX_RETRIES + ")");
                    try {
                        Thread.sleep(RETRY_DELAY_SECONDS * 1000);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        Log.error("Proxy server startup was interrupted. Aborting.", interruptedException);
                        return;
                    }
                } else {
                    Log.error("Could not bind to port " + PORT + " after " + MAX_RETRIES + " attempts. The server will not start.", bindException);
                }
            } catch (IOException ioException) {
                Log.error("An unexpected I/O error occurred while starting the proxy server.", ioException);
                return;
            }
        }
    }

    private static void stop() {
        if (server != null) {
            Log.info("Stopping Ollama forward proxy server...");
            server.stop(1);
            server = null;
            Log.info("Proxy server stopped.");
        }
    }
}