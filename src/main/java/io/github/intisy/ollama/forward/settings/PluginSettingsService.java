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
        public List<CustomLLM> customLLMs = new ArrayList<>();
    }

    private State myState = new State();

    public static PluginSettingsService getInstance() {
        return ApplicationManager.getApplication().getService(PluginSettingsService.class);
    }

    public List<CustomLLM> getCustomLLMs() {
        return myState.customLLMs;
    }

    public void setCustomLLMs(List<CustomLLM> llms) {
        myState.customLLMs = llms;
    }

    @Nullable
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }
}