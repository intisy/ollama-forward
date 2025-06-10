package io.github.intisy.ollama.forward.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GeminiClient implements ApiClient {
    private static final String GEMINI_API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-latest:streamGenerateContent?key=%s";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @Override
    public void handleChatRequest(String apiKey, Map<String, Object> requestPayload, HttpExchange exchange) throws IOException {
        String modelName = (String) requestPayload.get("model");
        Map<String, Object> geminiRequestPayload = Map.of(
                "contents", transformMessagesToContents((List<Map<String, String>>) requestPayload.get("messages"))
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(GEMINI_API_URL_TEMPLATE, apiKey)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(geminiRequestPayload)))
                .build();
        try {
            exchange.getResponseHeaders().set("Content-Type", "application/x-ndjson");
            exchange.sendResponseHeaders(200, 0);

            HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));
                 OutputStream responseBody = exchange.getResponseBody()) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String ollamaChunk = transformToOllamaChunk(line, modelName);
                    if (ollamaChunk != null) {
                        responseBody.write(ollamaChunk.getBytes(StandardCharsets.UTF_8));
                        responseBody.write('\n');
                        responseBody.flush();
                    }
                }
            }
            String finalChunk = createFinalOllamaChunk(modelName);
            exchange.getResponseBody().write(finalChunk.getBytes(StandardCharsets.UTF_8));
            exchange.getResponseBody().write('\n');
            exchange.getResponseBody().flush();
        } catch (InterruptedException e) {
            throw new IOException("Request to Gemini was interrupted", e);
        } finally {
            exchange.getResponseBody().close();
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

    private String transformToOllamaChunk(String geminiJsonChunk, String modelName) {
        JsonElement root = gson.fromJson(geminiJsonChunk, JsonElement.class);
        if (root.isJsonArray()) {
            root = root.getAsJsonArray().get(0);
        }
        JsonObject chunk = root.getAsJsonObject();

        if (chunk.has("candidates")) {
            JsonArray candidates = chunk.getAsJsonArray("candidates");
            if (!candidates.isEmpty()) {
                JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                if (content != null && content.has("parts")) {
                    JsonArray parts = content.getAsJsonArray("parts");
                    if (!parts.isEmpty() && parts.get(0).getAsJsonObject().has("text")) {
                        String text = parts.get(0).getAsJsonObject().get("text").getAsString();
                        Map<String, Object> ollamaResponse = Map.of(
                                "model", modelName,
                                "created_at", java.time.ZonedDateTime.now().toString(),
                                "response", text,
                                "done", false
                        );
                        return gson.toJson(ollamaResponse);
                    }
                }
            }
        }
        return null;
    }

    private String createFinalOllamaChunk(String modelName) {
        Map<String, Object> finalResponse = Map.of(
                "model", modelName,
                "created_at", java.time.ZonedDateTime.now().toString(),
                "response", "",
                "done", true,
                "total_duration", 1,
                "prompt_eval_count", 1,
                "eval_count", 1
        );
        return gson.toJson(finalResponse);
    }
}