package io.github.intisy.ollama.forward.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;

public class SecureStore {
    private static final String SERVICE_NAME = "OllamaForwardAPIKey";

    private CredentialAttributes getAttributes(String key) {
        return new CredentialAttributes(SERVICE_NAME + "_" + key);
    }

    public void saveApiKey(String providerId, String apiKey) {
        PasswordSafe.getInstance()
                .set(getAttributes(providerId), new Credentials(providerId, apiKey));
    }

    public String getApiKey(String providerId) {
        Credentials creds = PasswordSafe.getInstance().get(getAttributes(providerId));
        return creds != null ? creds.getPasswordAsString() : "";
    }

    public void removeApiKey(String providerId) {
        PasswordSafe.getInstance().set(getAttributes(providerId), null);
    }
}
