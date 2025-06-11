package io.github.intisy.ollama.forward.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBPasswordField;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class OllamaForwardSettingsConfigurable implements Configurable {
    private JPanel panel;
    private JBCheckBox debugCheckBox;
    private final Map<CustomLLM.ProviderType, JBCheckBox> enableMap = new EnumMap<>(CustomLLM.ProviderType.class);
    private final Map<CustomLLM.ProviderType, JBPasswordField> apiKeyMap = new EnumMap<>(CustomLLM.ProviderType.class);
    private final SecureStore secureStore = new SecureStore();
    private final PluginSettingsService settingsService = PluginSettingsService.getInstance();

    @Override
    public @Nls String getDisplayName() {
        return "Ollama Forward";
    }

    @Override
    public @Nullable JComponent createComponent() {
        panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        debugCheckBox = new JBCheckBox("Enable debug mode");
        panel.add(debugCheckBox, gbc);

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
        if (debugCheckBox.isSelected() != settingsService.isDebug()) return true;
        List<CustomLLM> current = settingsService.getProviders();
        for (CustomLLM.ProviderType type : CustomLLM.ProviderType.values()) {
            boolean e = enableMap.get(type).isSelected();
            String key = new String(apiKeyMap.get(type).getPassword());
            Optional<CustomLLM> exist = current.stream()
                    .filter(c -> c.getProviderType() == type)
                    .findFirst();
            if (exist.map(c -> c.isEnabled() != e).orElse(true)) return true;
            if (!Objects.equals(key, secureStore.getApiKey(type.name()))) return true;
        }
        return false;
    }

    @Override
    public void apply() {
        settingsService.setDebug(debugCheckBox.isSelected());
        List<CustomLLM> updated = new ArrayList<>();
        for (CustomLLM.ProviderType type : CustomLLM.ProviderType.values()) {
            boolean e = enableMap.get(type).isSelected();
            String key = new String(apiKeyMap.get(type).getPassword());
            updated.add(new CustomLLM(type, e));
            secureStore.saveApiKey(type.name(), key);
        }
        settingsService.setProviders(updated);
    }

    @Override
    public void reset() {
        debugCheckBox.setSelected(settingsService.isDebug());
        List<CustomLLM> current = settingsService.getProviders();
        for (CustomLLM.ProviderType type : CustomLLM.ProviderType.values()) {
            Optional<CustomLLM> c = current.stream()
                    .filter(x -> x.getProviderType() == type).findFirst();
            enableMap.get(type).setSelected(c.map(CustomLLM::isEnabled).orElse(false));
            apiKeyMap.get(type).setText(secureStore.getApiKey(type.name()));
        }
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        enableMap.clear();
        apiKeyMap.clear();
    }
}
