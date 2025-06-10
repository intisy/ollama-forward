package io.github.intisy.ollama.forward.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;

public class SecureStore {
    private static final String SERVICE_NAME_PREFIX = "OLLAMA_FORWARD_API_KEYS";

    private CredentialAttributes createCredentialAttributes(String modelName) {
        return new CredentialAttributes(SERVICE_NAME_PREFIX + "_" + modelName);
    }

    public void saveApiKey(String modelName, String apiKey) {
        CredentialAttributes attributes = createCredentialAttributes(modelName);
        Credentials credentials = new Credentials(modelName, apiKey);
        PasswordSafe.getInstance().set(attributes, credentials);
    }

    public String getApiKey(String modelName) {
        CredentialAttributes attributes = createCredentialAttributes(modelName);
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        return (credentials != null) ? credentials.getPasswordAsString() : null;
    }

    public void removeApiKey(String modelName) {
        CredentialAttributes attributes = createCredentialAttributes(modelName);
        PasswordSafe.getInstance().set(attributes, null);
    }
}