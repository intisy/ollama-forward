package io.github.intisy.ollama.forward.proxy.handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.intisy.ollama.forward.Server;
import io.github.intisy.ollama.forward.settings.CustomLLM;
import io.github.intisy.ollama.forward.settings.Settings;
import io.github.intisy.simple.logger.SimpleLogger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TagsHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String OLLAMA_REAL_URL = "http://localhost:11434/api/tags";

    private final Settings settings;
    private final SimpleLogger logger;

    public TagsHandler(Server server) {
        this.settings = server.getSettings();
        this.logger = server.getLogger();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes());
        logger.debug("TagsHandler request: " + body);
        List<Map<String,Object>> real = fetchReal();
        for (CustomLLM llm : settings.getProviders()) {
            if (!llm.isEnabled()) continue;
            real.add(Map.of(
                    "name", llm.getProviderType().name().toLowerCase() + ":" + llm.getModel(),
                    "model", llm.getProviderType().name().toLowerCase() + ":" + llm.getModel(),
                    "modified_at", "2000-01-01T00:00:00.000000Z",
                    "size", 0,
                    "digest", llm.getProviderType().name().hashCode(),
                    "details", Map.of(
                            "parent_model", "",
                            "format", "gguf",
                            "family", "",
                            "families", List.of(),
                            "parameter_size", "8.2B",
                            "quantization_level", "Q4_K_M"
                            )
            ));
        }
        String resp = gson.toJson(Map.of("models", real));
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, resp.getBytes().length);
        exchange.getResponseBody().write(resp.getBytes());
        exchange.getResponseBody().close();
    }

    private List<Map<String,Object>> fetchReal() {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(OLLAMA_REAL_URL)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Type type = new TypeToken<Map<String, List<Map<String, Object>>>>() {}.getType();
                Map<String, List<Map<String, Object>>> ollamaResponse = gson.fromJson(response.body(), type);
                return new ArrayList<>(ollamaResponse.getOrDefault("models", List.of()));
            }
        } catch (Exception ignored) {}
        return new ArrayList<>();
    }
}
