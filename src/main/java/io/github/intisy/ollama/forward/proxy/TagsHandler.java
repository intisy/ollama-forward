package io.github.intisy.ollama.forward.proxy;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.intisy.ollama.forward.settings.CustomLLM;
import io.github.intisy.ollama.forward.settings.PluginSettingsService;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TagsHandler implements HttpHandler {

    private final Gson gson = new Gson();
    private final PluginSettingsService settings = PluginSettingsService.getInstance();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String OLLAMA_REAL_URL = "http://localhost:11434/api/tags";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> realModels = getRealOllamaModels();
        List<CustomLLM> customLLMs = settings.getCustomLLMs();

        for (CustomLLM llm : customLLMs) {
            realModels.add(Map.of(
                    "name", llm.modelName() + ":latest",
                    "model", llm.modelName() + ":latest",
                    "modified_at", "2024-01-01T00:00:00.000000Z",
                    "size", 0,
                    "digest", "custom-" + llm.modelName().hashCode()
            ));
        }

        Map<String, Object> finalResponse = Map.of("models", realModels);
        String responseBody = gson.toJson(finalResponse);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBody.getBytes().length);
        exchange.getResponseBody().write(responseBody.getBytes());
        exchange.getResponseBody().close();
    }

    private List<Map<String, Object>> getRealOllamaModels() {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(OLLAMA_REAL_URL)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Type type = new TypeToken<Map<String, List<Map<String, Object>>>>() {}.getType();
                Map<String, List<Map<String, Object>>> ollamaResponse = gson.fromJson(response.body(), type);
                return new ArrayList<>(ollamaResponse.getOrDefault("models", List.of()));
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Could not connect to real Ollama server: " + e.getMessage());
        }
        return new ArrayList<>();
    }
}