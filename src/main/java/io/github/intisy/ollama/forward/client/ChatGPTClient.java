package io.github.intisy.ollama.forward.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import io.github.intisy.ollama.forward.Server;
import io.github.intisy.simple.logger.SimpleLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

public class ChatGPTClient implements ApiClient {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    protected final HttpClient httpClient = HttpClient.newHttpClient();
    protected final Gson gson = new Gson();

    protected String getApiUrl() {
        return OPENAI_API_URL;
    }

    @Override
    public void handleChatRequest(Server server, String model, String apiKey, Map<String, Object> requestPayload, HttpExchange exchange) throws IOException {
        SimpleLogger logger = server.getLogger();
        logger.debug("ChatGPTClient request: " + gson.toJson(requestPayload));

        String modelName = (String) requestPayload.get("model");
        Map<String, Object> apiRequestPayload = Map.of(
                "model", model,
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
                            logger.debug("ChatGPT response chunk: " + ollamaChunk);
                            responseBody.write(ollamaChunk.getBytes(StandardCharsets.UTF_8));
                            responseBody.write('\n');
                            responseBody.flush();
                        }
                    }
                }

                String finalChunk = createFinalOllamaChunk(modelName, true);
                logger.debug("ChatGPT final response chunk: " + finalChunk);
                responseBody.write(finalChunk.getBytes(StandardCharsets.UTF_8));
                responseBody.write('\n');
                responseBody.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request to API was interrupted", e);
        }
    }

    private String transformToOllamaChunk(String apiJsonChunk, String modelName) {
        JsonObject chunk = gson.fromJson(apiJsonChunk, JsonObject.class);
        if (chunk.has("choices")) {
            JsonObject delta = chunk.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("delta");
            if (delta.has("content") && !delta.get("content").isJsonNull()) {
                String content = delta.get("content").getAsString();
                if (content.isEmpty()) {
                    return null;
                }

                Map<String, Object> messageMap = Map.of(
                        "role", "assistant",
                        "content", content
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
        return null;
    }

    private String createFinalOllamaChunk(String modelName, boolean done) {
        Map<String, Object> finalMessageMap = Map.of(
                "role", "assistant",
                "content", ""
        );
        Map<String, Object> finalResponse = Map.of(
                "model", modelName,
                "created_at", Instant.now().toString(),
                "message", finalMessageMap,
                "done", done,
                "total_duration", 1,
                "prompt_eval_count", 1,
                "eval_count", 1
        );
        return gson.toJson(finalResponse);
    }
}