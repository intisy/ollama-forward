package io.github.intisy.ollama.forward.proxy;

import io.github.intisy.simple.logger.Log;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.intisy.ollama.forward.client.*;
import io.github.intisy.ollama.forward.settings.CustomLLM;
import io.github.intisy.ollama.forward.settings.PluginSettingsService;
import io.github.intisy.ollama.forward.settings.SecureStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.Map;
import java.util.Optional;

public class ChatHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final PluginSettingsService settings = PluginSettingsService.getInstance();
    private final SecureStore secureStore = new SecureStore();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Map<CustomLLM.ProviderType, ApiClient> clients = Map.of(
            CustomLLM.ProviderType.OPENAI, new OpenAiClient(),
            CustomLLM.ProviderType.GEMINI, new GeminiClient(),
            CustomLLM.ProviderType.DEEPSEEK, new DeepSeekClient()
    );
    private static final String OLLAMA_REAL_URL = "http://localhost:11434/api/chat";

    public ChatHandler() {
        if (settings.isDebug()) {
            Log.debug("ChatHandler initialized");
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        boolean debug = settings.isDebug();
        String body = new String(exchange.getRequestBody().readAllBytes());
        if (debug) Log.debug("ChatHandler request: " + body);
        Map<String, Object> req = gson.fromJson(body, Map.class);
        String model = (String) req.get("model");

        Optional<CustomLLM> custom = settings.getProviders().stream()
                .filter(m -> m.isEnabled() && m.getProviderType().name().equalsIgnoreCase(model))
                .findFirst();

        if (custom.isPresent()) {
            if (debug) Log.debug("Using custom model: " + custom.get().getProviderType());
            ApiClient client = clients.get(custom.get().getProviderType());
            String key = secureStore.getApiKey(custom.get().getProviderType().name());
            if (key.isBlank()) {
                respond(exchange, 401, "API Key not configured");
            } else {
                client.handleChatRequest(key, req, exchange);
            }
        } else {
            if (debug) Log.debug("Forwarding to Ollama real API");
            forward(exchange, body);
        }
    }

    private void forward(HttpExchange ex, String body) throws IOException {
        try {
            HttpRequest r = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_REAL_URL))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<byte[]> resp = httpClient.send(r, HttpResponse.BodyHandlers.ofByteArray());
            ex.getResponseHeaders().putAll(resp.headers().map());
            ex.sendResponseHeaders(resp.statusCode(), resp.body().length);
            ex.getResponseBody().write(resp.body());
            ex.getResponseBody().close();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private void respond(HttpExchange ex, int code, String msg) throws IOException {
        ex.sendResponseHeaders(code, msg.length());
        ex.getResponseBody().write(msg.getBytes());
        ex.getResponseBody().close();
    }
}
