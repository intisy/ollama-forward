package io.github.intisy.ollama.forward.proxy;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.sun.net.httpserver.HttpServer;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ProxyServerManager implements ProjectActivity {
    private static final int PROXY_PORT = 11435;
    private static HttpServer server;

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        startServer();
        return Unit.INSTANCE;
    }

    private void startServer() {
        if (server != null) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", PROXY_PORT), 0);
            server.createContext("/api/tags", new TagsHandler());
            server.createContext("/api/chat", new ChatHandler());
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();
            System.out.println("Ollama Forwarding Proxy started on port " + PROXY_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}