package io.github.intisy.ollama.forward.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(
        name = "io.github.intisy.ollama.forward.settings.PluginSettingsState",
        storages = @Storage("OllamaForwardSettings.xml")
)
public class PluginSettingsService implements PersistentStateComponent<PluginSettingsService.State> {
    public static class State {
        public boolean debug = true;
        public List<CustomLLM> providers = new ArrayList<>();
    }

    private final State myState = new State();

    public static PluginSettingsService getInstance() {
        return ApplicationManager.getApplication().getService(PluginSettingsService.class);
    }

    @Override
    public @Nullable State getState() { return myState; }

    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }

    public boolean isDebug() { return myState.debug; }
    public void setDebug(boolean debug) { myState.debug = debug; }

    public List<CustomLLM> getProviders() { return myState.providers; }
    public void setProviders(List<CustomLLM> providers) { myState.providers = providers; }
}
