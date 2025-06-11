package io.github.intisy.ollama.forward.proxy;

import com.esotericsoftware.kryo.kryo5.minlog.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.intisy.ollama.forward.settings.CustomLLM;
import io.github.intisy.ollama.forward.settings.PluginSettingsService;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TagsHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final PluginSettingsService settings = PluginSettingsService.getInstance();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String OLLAMA_REAL_URL = "http://localhost:11434/api/tags";

    public TagsHandler() {
        if (settings.isDebug()) {
            Log.debug("[DEBUG] TagsHandler initialized");
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        boolean debug = settings.isDebug();
        if (debug) Log.debug("[DEBUG] TagsHandler called");
        List<Map<String,Object>> real = fetchReal();
        for (CustomLLM llm : settings.getProviders()) {
            if (!llm.isEnabled()) continue;
            real.add(Map.of(
                    "name", llm.getProviderType().name() + ":latest",
                    "model", llm.getProviderType().name() + ":latest",
                    "modified_at", "2024-01-01T00:00:00.000000Z",
                    "size", 0,
                    "digest", "custom-" + llm.getProviderType().name().hashCode()
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
