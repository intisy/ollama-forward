package io.github.intisy.ollama.forward.proxy.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.intisy.ollama.forward.Server;
import io.github.intisy.simple.logger.SimpleLogger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ForwardingHandler implements HttpHandler {
    private final String path;
    private final String targetUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String OLLAMA_BASE_URL = "http://localhost:11434";

    private final SimpleLogger logger;

    public ForwardingHandler(Server server, String path) {
        this.logger = server.getLogger();
        this.path = path;
        this.targetUrl = OLLAMA_BASE_URL + path;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        logger.debug("Forwarding request for " + this.path);
        try {
            byte[] body = exchange.getRequestBody().readAllBytes();
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(targetUrl));

            builder.method(exchange.getRequestMethod(), HttpRequest.BodyPublishers.ofByteArray(body));

            exchange.getRequestHeaders().forEach((header, values) -> {
                if (!header.equalsIgnoreCase("Host") && !header.equalsIgnoreCase("Content-Length")) {
                    values.forEach(value -> builder.header(header, value));
                }
            });

            HttpRequest request = builder.build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            exchange.getResponseHeaders().putAll(response.headers().map());
            exchange.sendResponseHeaders(response.statusCode(), response.body() != null ? response.body().length : -1);

            if (response.body() != null) {
                exchange.getResponseBody().write(response.body());
            }
            exchange.getResponseBody().close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request forwarding was interrupted for path: " + path, e);
        } catch (Exception e) {
            logger.error("Error forwarding request for " + path, e);
            String errorMsg = "Error forwarding request.";
            exchange.sendResponseHeaders(500, errorMsg.length());
            exchange.getResponseBody().write(errorMsg.getBytes());
            exchange.getResponseBody().close();
        }
    }
}
