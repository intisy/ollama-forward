package io.github.intisy.ollama.forward.proxy;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.intisy.ollama.forward.client.ApiClient;
import io.github.intisy.ollama.forward.client.DeepSeekClient;
import io.github.intisy.ollama.forward.client.GeminiClient;
import io.github.intisy.ollama.forward.client.OpenAiClient;
import io.github.intisy.ollama.forward.settings.CustomLLM;
import io.github.intisy.ollama.forward.settings.PluginSettingsService;
import io.github.intisy.ollama.forward.settings.SecureStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

public class ChatHandler implements HttpHandler {

    private final Gson gson = new Gson();
    private final PluginSettingsService settings = PluginSettingsService.getInstance();
    private final SecureStore secureStore = new SecureStore();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Map<CustomLLM.ProviderType, ApiClient> clients;
    private static final String OLLAMA_REAL_URL = "http://localhost:11434/api/chat";

    public ChatHandler() {
        clients = Map.of(
                CustomLLM.ProviderType.OPENAI, new OpenAiClient(),
                CustomLLM.ProviderType.GEMINI, new GeminiClient(),
                CustomLLM.ProviderType.DEEPSEEK, new DeepSeekClient()
        );
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestBody = new String(exchange.getRequestBody().readAllBytes());
        Map<String, Object> requestJson = gson.fromJson(requestBody, Map.class);
        String modelName = (String) requestJson.get("model");

        Optional<CustomLLM> customLLM = settings.getCustomLLMs().stream()
                .filter(m -> m.modelName().equalsIgnoreCase(modelName))
                .findFirst();

        if (customLLM.isPresent()) {
            handleCustomModel(customLLM.get(), requestJson, exchange);
        } else {
            forwardToOllama(requestBody, exchange);
        }
    }

    private void handleCustomModel(CustomLLM llm, Map<String, Object> request, HttpExchange exchange) throws IOException {
        ApiClient client = clients.get(llm.providerType());
        if (client == null) {
            String error = "Provider " + llm.providerType() + " is not supported.";
            exchange.sendResponseHeaders(501, error.length());
            exchange.getResponseBody().write(error.getBytes());
            exchange.getResponseBody().close();
            return;
        }

        String apiKey = secureStore.getApiKey(llm.modelName());
        if (apiKey == null || apiKey.isBlank()) {
            String error = "API Key for " + llm.modelName() + " is not configured.";
            exchange.sendResponseHeaders(401, error.length());
            exchange.getResponseBody().write(error.getBytes());
            exchange.getResponseBody().close();
            return;
        }

        client.handleChatRequest(apiKey, request, exchange);
    }

    private void forwardToOllama(String requestBody, HttpExchange exchange) throws IOException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_REAL_URL))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            exchange.getResponseHeaders().putAll(response.headers().map());
            exchange.sendResponseHeaders(response.statusCode(), response.body().length);
            exchange.getResponseBody().write(response.body());
            exchange.getResponseBody().close();
        } catch (InterruptedException e) {
            throw new IOException("Forwarding to Ollama was interrupted", e);
        }
    }
}