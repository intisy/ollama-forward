package io.github.intisy.ollama.forward.client;

import com.sun.net.httpserver.HttpExchange;
import io.github.intisy.ollama.forward.Server;

import java.io.IOException;
import java.util.Map;

public interface ApiClient {
    void handleChatRequest(Server server, String model, String apiKey, Map<String, Object> requestPayload, HttpExchange exchange) throws IOException;
}