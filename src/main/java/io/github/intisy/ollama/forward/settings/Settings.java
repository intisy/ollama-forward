package io.github.intisy.ollama.forward.settings;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Finn Birich
 */
public class Settings {
    private SettingsService settingsService;
    private SettingsService.State state;
    private boolean test;

    public void setTest(boolean test) {
        this.test = test;
    }

    public boolean isTest() {
        return test;
    }

    public @NotNull SettingsService getSettingsService() {
        if (settingsService == null)
            settingsService = ApplicationManager.getApplication().getService(SettingsService.class);
        return settingsService;
    }

    public SettingsService.State getState() {
        if (!isTest())
            return getSettingsService().getState();
        else if (state == null) {
            state = new SettingsService.State();
            state.providers = new ArrayList<>();
            for (CustomLLM.ProviderType type : CustomLLM.ProviderType.values()) {
                for (String model : type.getModels()) {
                    state.providers.add(new CustomLLM(type, model, true));
                }
            }
        }
        return state;
    }

    public void loadState(@NotNull SettingsService.State state) {
        if (isTest())
            throw new UnsupportedOperationException("Cannot load state in non-test mode");
        getSettingsService().loadState(state);
    }

    public List<CustomLLM> getProviders() {
        return getState().providers;
    }
    public void setProviders(List<CustomLLM> providers) {
        getState().providers = providers;
    }
}
