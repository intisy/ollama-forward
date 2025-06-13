package io.github.intisy.ollama.forward.settings;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigLoader {

    private static final String SECRETS_FILE_NAME = "secrets.properties";
    private final Properties properties;
    private final Path secretsFilePath;

    public ConfigLoader() {
        this.secretsFilePath = Paths.get(SECRETS_FILE_NAME);
        ensureSecretsFileExists();

        this.properties = new Properties();
        try (InputStream input = new FileInputStream(secretsFilePath.toFile())) {
            properties.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Could not read " + SECRETS_FILE_NAME, ex);
        }
    }

    private void ensureSecretsFileExists() {
        if (Files.exists(secretsFilePath)) {
            return;
        }

        try {
            Files.createFile(secretsFilePath);
        } catch (IOException e) {
            throw new RuntimeException("FATAL ERROR: Could not create '" + SECRETS_FILE_NAME + "'. Check permissions.", e);
        }
    }

    public synchronized String getProperty(String key) {
        if (properties.containsKey(key)) {
            return properties.getProperty(key);
        }

        properties.setProperty(key, "");

        try (FileOutputStream out = new FileOutputStream(secretsFilePath.toFile())) {
            properties.store(out, "Automatically updated to add missing key: " + key);
        } catch (IOException e) {
            throw new RuntimeException("ERROR: Could not write new key '" + key + "' to " + SECRETS_FILE_NAME);
        }

        return null;
    }
}