package io.github.intisy.ollama.forward.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class OllamaForwardSettingsConfigurable implements Configurable {
    private JPanel panel;
    private final Map<CustomLLM.ProviderType, JBCheckBox> enableMap = new EnumMap<>(CustomLLM.ProviderType.class);
    private final Map<CustomLLM.ProviderType, JBPasswordField> apiKeyMap = new EnumMap<>(CustomLLM.ProviderType.class);
    private final SecureStoreService secureStoreService = new SecureStoreService();
    private final Settings settingsService = new Settings();

    private final Map<CustomLLM.ProviderType, Boolean> originalEnableState = new EnumMap<>(CustomLLM.ProviderType.class);
    private final Map<CustomLLM.ProviderType, String> originalApiKeys = new EnumMap<>(CustomLLM.ProviderType.class);


    @Override
    public @Nls String getDisplayName() {
        return "Ollama Forward";
    }

    @Override
    public @Nullable JComponent createComponent() {
        panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.insets = JBUI.insets(5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        for (CustomLLM.ProviderType type : CustomLLM.ProviderType.values()) {
            JPanel row = new JPanel(new BorderLayout());
            JBCheckBox cb = new JBCheckBox("Enable " + type.name());
            JBPasswordField pf = new JBPasswordField();
            row.add(cb, BorderLayout.WEST);
            row.add(pf, BorderLayout.CENTER);

            enableMap.put(type, cb);
            apiKeyMap.put(type, pf);
            panel.add(row, gbc);
        }
        return panel;
    }

    @Override
    public boolean isModified() {
        for (CustomLLM.ProviderType type : CustomLLM.ProviderType.values()) {
            if (enableMap.get(type).isSelected() != originalEnableState.getOrDefault(type, false)) {
                return true;
            }
            String currentApiKey = new String(apiKeyMap.get(type).getPassword());
            String originalApiKey = originalApiKeys.get(type);
            if (!Objects.equals(currentApiKey, originalApiKey)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void apply() {
        List<CustomLLM> updatedProviders = new ArrayList<>();
        Map<CustomLLM.ProviderType, String> newApiKeys = new EnumMap<>(CustomLLM.ProviderType.class);
        Map<CustomLLM.ProviderType, Boolean> newEnableStates = new EnumMap<>(CustomLLM.ProviderType.class);

        for (CustomLLM.ProviderType type : CustomLLM.ProviderType.values()) {
            boolean isEnabled = enableMap.get(type).isSelected();
            String apiKey = new String(apiKeyMap.get(type).getPassword());

            for (String model : type.getModels()) {
                updatedProviders.add(new CustomLLM(type, model, isEnabled));
            }
            newApiKeys.put(type, apiKey);
            newEnableStates.put(type, isEnabled);
        }

        settingsService.setProviders(updatedProviders);
        for (CustomLLM.ProviderType type : CustomLLM.ProviderType.values()) {
            secureStoreService.saveApiKey(type.name(), newApiKeys.get(type));
        }

        this.originalEnableState.putAll(newEnableStates);
        this.originalApiKeys.putAll(newApiKeys);
    }

    @Override
    public void reset() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<CustomLLM> providers = settingsService.getProviders();
            Map<CustomLLM.ProviderType, String> loadedApiKeys = new EnumMap<>(CustomLLM.ProviderType.class);
            Map<CustomLLM.ProviderType, Boolean> loadedEnableStates = new EnumMap<>(CustomLLM.ProviderType.class);

            for (CustomLLM.ProviderType type : CustomLLM.ProviderType.values()) {
                String apiKey = secureStoreService.getApiKey(type.name());
                loadedApiKeys.put(type, apiKey);

                boolean isEnabled = providers.stream()
                        .filter(p -> p.getProviderType() == type)
                        .findFirst()
                        .map(CustomLLM::isEnabled)
                        .orElse(false);
                loadedEnableStates.put(type, isEnabled);
            }

            ApplicationManager.getApplication().invokeLater(() -> {
                this.originalEnableState.clear();
                this.originalEnableState.putAll(loadedEnableStates);
                this.originalApiKeys.clear();
                this.originalApiKeys.putAll(loadedApiKeys);

                for (CustomLLM.ProviderType type : CustomLLM.ProviderType.values()) {
                    enableMap.get(type).setSelected(this.originalEnableState.get(type));
                    apiKeyMap.get(type).setText(this.originalApiKeys.get(type));
                }
            });
        });
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        enableMap.clear();
        apiKeyMap.clear();
        originalEnableState.clear();
        originalApiKeys.clear();
    }
}