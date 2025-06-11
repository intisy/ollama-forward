package io.github.intisy.ollama.forward.settings;

import com.intellij.util.xmlb.annotations.Attribute;

public class CustomLLM {
    public enum ProviderType { OPENAI, GEMINI, DEEPSEEK }
    @Attribute("providerType")
    private ProviderType providerType;
    @Attribute("enabled")
    private boolean enabled;

    public CustomLLM() {}
    public CustomLLM(ProviderType providerType, boolean enabled) {
        this.providerType = providerType;
        this.enabled = enabled;
    }

    public ProviderType getProviderType() { return providerType; }
    public void setProviderType(ProviderType providerType) { this.providerType = providerType; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
