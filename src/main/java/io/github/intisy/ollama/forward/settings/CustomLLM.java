package io.github.intisy.ollama.forward.settings;

public record CustomLLM(String modelName, ProviderType providerType) {
    public enum ProviderType {
        OPENAI,
        GEMINI,
        DEEPSEEK
    }
}