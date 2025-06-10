package io.github.intisy.ollama.forward.client;

import com.google.gson.Gson;
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
import java.util.Map;

public class OpenAiClient implements ApiClient {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    protected final HttpClient httpClient = HttpClient.newHttpClient();
    protected final Gson gson = new Gson();

    protected String getApiUrl() {
        return OPENAI_API_URL;
    }

    @Override
    public void handleChatRequest(String apiKey, Map<String, Object> requestPayload, HttpExchange exchange) throws IOException {
        String modelName = (String) requestPayload.get("model");
        Map<String, Object> apiRequestPayload = Map.of(
                "model", "gpt-4o",
                "messages", requestPayload.get("messages"),
                "stream", true
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getApiUrl()))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(apiRequestPayload)))
                .build();

        try {
            exchange.getResponseHeaders().set("Content-Type", "application/x-ndjson");
            exchange.sendResponseHeaders(200, 0);

            HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));
                 OutputStream responseBody = exchange.getResponseBody()) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        String ollamaChunk = transformToOllamaChunk(data, modelName);
                        if (ollamaChunk != null) {
                            responseBody.write(ollamaChunk.getBytes(StandardCharsets.UTF_8));
                            responseBody.write('\n');
                            responseBody.flush();
                        }
                    }
                }
            }
            String finalChunk = createFinalOllamaChunk(modelName);
            exchange.getResponseBody().write(finalChunk.getBytes(StandardCharsets.UTF_8));
            exchange.getResponseBody().write('\n');
            exchange.getResponseBody().flush();
        } catch (InterruptedException e) {
            throw new IOException("Request to API was interrupted", e);
        } finally {
            exchange.getResponseBody().close();
        }
    }

    private String transformToOllamaChunk(String apiJsonChunk, String modelName) {
        JsonObject chunk = gson.fromJson(apiJsonChunk, JsonObject.class);
        if (chunk.has("choices")) {
            JsonObject delta = chunk.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("delta");
            if (delta.has("content") && !delta.get("content").isJsonNull()) {
                String content = delta.get("content").getAsString();
                Map<String, Object> ollamaResponse = Map.of(
                        "model", modelName,
                        "created_at", java.time.ZonedDateTime.now().toString(),
                        "response", content,
                        "done", false
                );
                return gson.toJson(ollamaResponse);
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