package io.github.intisy.ollama.forward.client;

public class DeepSeekClient extends OpenAiClient {
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";

    @Override
    protected String getApiUrl() {
        return DEEPSEEK_API_URL;
    }
}