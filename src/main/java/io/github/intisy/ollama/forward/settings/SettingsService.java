package io.github.intisy.ollama.forward.settings;

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
public class SettingsService implements PersistentStateComponent<SettingsService.State> {
    public static class State {
        public List<CustomLLM> providers = new ArrayList<>();
    }

    private final State myState = new State();

    @Override
    public @Nullable State getState() { return myState; }

    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }

    public List<CustomLLM> getProviders() { return myState.providers; }
    public void setProviders(List<CustomLLM> providers) { myState.providers = providers; }
}
