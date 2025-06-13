package io.github.intisy.ollama.forward.proxy.handler;

import io.github.intisy.ollama.forward.Server;
import io.github.intisy.ollama.forward.settings.*;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.intisy.ollama.forward.client.*;
import io.github.intisy.simple.logger.SimpleLogger;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.Map;
import java.util.Optional;

public class ChatHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Map<CustomLLM.ProviderType, ApiClient> clients = Map.of(
            CustomLLM.ProviderType.CHATGPT, new ChatGPTClient(),
            CustomLLM.ProviderType.GEMINI, new GeminiClient(),
            CustomLLM.ProviderType.DEEPSEEK, new DeepSeekClient()
    );
    private static final String OLLAMA_REAL_URL = "http://localhost:11434/api/chat";

    private final Server server;
    private final Settings settings;
    private final SimpleLogger logger;
    private final SecureStore secureStore;

    public ChatHandler(Server server) {
        this.server = server;
        this.settings = server.getSettings();
        this.logger = server.getLogger();
        this.secureStore = server.getSecureStore();
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes());
            logger.debug("ChatHandler request: " + body);
            Map<String, Object> req = gson.fromJson(body, Map.class);
            String model = (String) req.get("model");

            Optional<CustomLLM> custom = settings.getProviders().stream()
                    .filter(m -> m.isEnabled() && m.getProviderType().name().equalsIgnoreCase(model.split(":" + m.getModel())[0]))
                    .findFirst();

            if (custom.isPresent()) {
                logger.debug("Using custom model: " + custom.get().getProviderType());
                ApiClient client = clients.get(custom.get().getProviderType());
                String key = secureStore.getApiKey(custom.get().getProviderType().name());
                if (key == null || key.isBlank()) {
                    logger.debug("API Key not configured for " + custom.get().getProviderType());
                    respond(exchange, 401, "API Key not configured");
                } else {
                    client.handleChatRequest(server, custom.get().getModel(), key, req, exchange);
                }
            } else {
                logger.debug("Forwarding to Ollama real API");
                forward(exchange, body);
            }
        } catch (IOException e) {
            logger.error("Error handling request", e);
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
            logger.debug("Forwarded response chunk: " + new String(resp.body()));
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
