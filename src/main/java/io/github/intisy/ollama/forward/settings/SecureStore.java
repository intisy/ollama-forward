package io.github.intisy.ollama.forward.settings;

import com.intellij.credentialStore.CredentialAttributes;

/**
 * @author Finn Birich
 */
public class SecureStore {
    private SecureStoreService service;
    private ConfigLoader configLoader;
    private boolean test;

    public void setTest(boolean test) {
        this.test = test;
    }

    public boolean isTest() {
        return test;
    }

    private SecureStoreService getService() {
        if (service == null) {
            service = new SecureStoreService();
        }
        return service;
    }

    private ConfigLoader getConfigLoader() {
        if (configLoader == null) {
            configLoader = new ConfigLoader();
        }
        return configLoader;
    }

    private CredentialAttributes getAttributes(String key) {
        if (isTest()) {
            return null;
        } else {
            return getService().getAttributes(key);
        }
    }

    public void saveApiKey(String providerId, String apiKey) {
        if (!isTest())
            getService().saveApiKey(providerId, apiKey);
    }

    public String getApiKey(String providerId) {
        if (isTest()) {
            return getConfigLoader().getProperty(providerId);
        } else {
            return getService().getApiKey(providerId);
        }
    }

    public void removeApiKey(String providerId) {
        if (!isTest())
            getService().removeApiKey(providerId);
    }
}
