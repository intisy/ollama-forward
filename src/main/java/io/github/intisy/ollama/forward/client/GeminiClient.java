package io.github.intisy.ollama.forward.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.sun.net.httpserver.HttpExchange;
import io.github.intisy.ollama.forward.Server;
import io.github.intisy.simple.logger.SimpleLogger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GeminiClient implements ApiClient {
    private static final String GEMINI_API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-%s:streamGenerateContent?key=%s";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @Override
    public void handleChatRequest(Server server, String model, String apiKey, Map<String, Object> requestPayload, HttpExchange exchange) throws IOException {
        SimpleLogger logger = server.getLogger();
        logger.debug("GeminiClient request: " + gson.toJson(requestPayload));

        String modelName = (String) requestPayload.get("model");
        Map<String, Object> geminiRequestPayload = Map.of(
                "contents", transformMessagesToContents((List<Map<String, String>>) requestPayload.get("messages"))
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(GEMINI_API_URL_TEMPLATE, model, apiKey)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(geminiRequestPayload)))
                .build();

        try {
            exchange.getResponseHeaders().set("Content-Type", "application/x-ndjson");
            exchange.sendResponseHeaders(200, 0);

            HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (OutputStream responseBody = exchange.getResponseBody();
                 InputStreamReader isr = new InputStreamReader(response.body(), StandardCharsets.UTF_8);
                 JsonReader jsonReader = new JsonReader(isr)) {

                jsonReader.beginArray();
                while (jsonReader.hasNext()) {
                    JsonObject geminiChunkObject = gson.fromJson(jsonReader, JsonObject.class);
                    String ollamaChunk = transformToOllamaChunk(geminiChunkObject, modelName);
                    if (ollamaChunk != null) {
                        logger.debug("Gemini response chunk: " + ollamaChunk);
                        responseBody.write(ollamaChunk.getBytes(StandardCharsets.UTF_8));
                        responseBody.write('\n');
                        responseBody.flush();
                    }
                }

                String finalChunk = createFinalOllamaChunk(modelName);
                responseBody.write(finalChunk.getBytes(StandardCharsets.UTF_8));
                responseBody.write('\n');
                responseBody.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request to Gemini was interrupted", e);
        }
    }

    private List<Map<String, Object>> transformMessagesToContents(List<Map<String, String>> messages) {
        List<Map<String, Object>> contents = new ArrayList<>();
        for (Map<String, String> message : messages) {
            String role = message.get("role").equalsIgnoreCase("assistant") ? "model" : "user";
            contents.add(Map.of(
                    "role", role,
                    "parts", List.of(Map.of("text", message.get("content")))
            ));
        }
        return contents;
    }

    private String transformToOllamaChunk(JsonObject geminiChunk, String modelName) {
        if (geminiChunk.has("candidates")) {
            JsonArray candidates = geminiChunk.getAsJsonArray("candidates");
            if (!candidates.isEmpty()) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    if (content.has("parts")) {
                        JsonArray parts = content.getAsJsonArray("parts");
                        if (!parts.isEmpty() && parts.get(0).getAsJsonObject().has("text")) {
                            String text = parts.get(0).getAsJsonObject().get("text").getAsString();

                            Map<String, Object> messageMap = Map.of(
                                    "role", "assistant",
                                    "content", text
                            );
                            Map<String, Object> ollamaResponse = Map.of(
                                    "model", modelName,
                                    "created_at", Instant.now().toString(),
                                    "message", messageMap,
                                    "done", false
                            );
                            return gson.toJson(ollamaResponse);
                        }
                    }
                }
            }
        }
        return null;
    }

    private String createFinalOllamaChunk(String modelName) {
        Map<String, Object> finalMessageMap = Map.of(
                "role", "assistant",
                "content", ""
        );
        Map<String, Object> finalResponse = Map.of(
                "model", modelName,
                "created_at", Instant.now().toString(),
                "message", finalMessageMap,
                "done", true,
                "total_duration", 1,
                "prompt_eval_count", 1,
                "eval_count", 1
        );
        return gson.toJson(finalResponse);
    }
}