package io.github.intisy.ollama.forward.proxy;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import io.github.intisy.ollama.forward.Server;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProxyServerManager implements ProjectActivity {
    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        Server server = new Server();
        synchronized (ProxyServerManager.class) {
            server.start();
        }
        return Unit.INSTANCE;
    }
}