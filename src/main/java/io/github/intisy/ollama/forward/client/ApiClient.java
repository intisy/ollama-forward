package io.github.intisy.ollama.forward.client;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.Map;

public interface ApiClient {
    void handleChatRequest(String apiKey, Map<String, Object> requestPayload, HttpExchange exchange) throws IOException;
}