package io.github.intisy.ollama.forward.settings;

import com.intellij.util.xmlb.annotations.Attribute;

public class CustomLLM {
    public enum ProviderType {
        CHATGPT("gpt-4o"),
        GEMINI("2.5-pro-preview-06-05", "2.5-pro-preview-05-06", "2.5-flash-preview-04-17", "2.5-flash-preview-04-17", "2.0-flash", "2.0-flash-lite"),
        DEEPSEEK();

        final private String[] models;
        ProviderType(String... models) {
            this.models = models;
        }

        public String[] getModels() {
            return models;
        }
    }
    @Attribute("providerType")
    private ProviderType providerType;
    @Attribute("model")
    private String model;
    @Attribute("enabled")
    private boolean enabled;

    public CustomLLM() {}
    public CustomLLM(ProviderType providerType, String model, boolean enabled) {
        this.providerType = providerType;
        this.model = model;
        this.enabled = enabled;
    }

    public ProviderType getProviderType() { return providerType; }
    public void setProviderType(ProviderType providerType) { this.providerType = providerType; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public String toString() {
        return "CustomLLM{" +
                "providerType=" + providerType +
                ", model=" + model +
                ", enabled=" + enabled +
                '}';
    }
}
