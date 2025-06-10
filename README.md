# Ollama Forward

[![Build](https://github.com/intisy/ollama-forward/actions/workflows/build.yml/badge.svg)](https://github.com/intisy/ollama-forward/actions/workflows/build.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Compatibility](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

**Supercharge the JetBrains AI Assistant by running models like GPT-4, Gemini 1.5 Pro, and DeepSeek through your local Ollama instance using your own API keys.**

<!-- Plugin description -->
This plugin acts as a bridge between the JetBrains AI Assistant and powerful, proprietary AI models. It allows you to configure models like OpenAI's ChatGPT, Google's Gemini, and DeepSeek as local models within your Ollama instance.

By doing so, you can use these advanced models directly inside the JetBrains AI Assistant, all while leveraging your own API keys for privacy, control, and access to the latest versions. The plugin provides a simple GUI to manage your models and securely stores your API keys in the IDE's password safe.
<!-- Plugin description end -->

## The Problem It Solves

The JetBrains AI Assistant is fantastic, but you are limited to the models it provides. Ollama is excellent for running local, open-source models. This plugin combines the best of both worlds:

*   **Use Any Model:** Access powerful models like GPT-4o, Gemini 1.5 Pro, and others that are not natively available in the AI Assistant.
*   **Use Your Own API Keys:** Avoid shared rate limits and potential data privacy concerns by using your own personal or team API keys.
*   **Seamless Integration:** Once configured, your custom models appear as regular Ollama models, available for selection directly within the JetBrains AI Assistant chat.
*   **Simplicity:** No need to manually create complex `Modelfile` configurations or run a separate proxy server. The plugin handles it all.

## Features

*   **Simple GUI:** A dedicated tool window to add, configure, and remove custom API-backed models.
*   **Major Provider Support:** Built-in support for OpenAI (ChatGPT), Google (Gemini), and DeepSeek.
*   **Secure API Key Storage:** API keys are stored securely using the IDE's built-in password safe.
*   **Automatic Ollama Configuration:** The plugin dynamically configures Ollama to proxy requests for your custom models to the appropriate cloud API.

## Prerequisites

1.  **Ollama:** You must have the [Ollama](https://ollama.com/) application installed and running on your local machine.
2.  **JetBrains AI Assistant:** The AI Assistant plugin must be installed and enabled in your IDE.
3.  **API Keys:** You need your own API key for each service you wish to use (e.g., an OpenAI API key).

## Installation

1.  Go to `Settings / Preferences` > `Plugins`.
2.  Search for "Ollama Forward" in the Marketplace tab.
3.  Click `Install` and restart the IDE if prompted.

*(Note: Once published, the links at the top of this README will need to be updated with your plugin's numeric ID from the JetBrains Marketplace.)*

## Getting Started: A Step-by-Step Guide

### Step 1: Configure AI Assistant to Use Ollama
First, you must tell the JetBrains AI Assistant to use your local Ollama instance as its provider.

1.  Open the AI Assistant settings: `Settings / Preferences` > `Tools` > `AI Assistant`.
2.  Under "LLM Provider," select **Ollama**.
3.  Ensure the Ollama server URL is correct (usually `http://localhost:11434`).

### Step 2: Add a Custom Model
Now, use this plugin to add a cloud model (like ChatGPT) to your Ollama instance.

1.  Open the Ollama Forward tool window: `View` > `Tool Windows` > `Ollama Forward`.
2.  Click the `+` (Add Model) button.
3.  In the dialog:
*   **Model Name:** Give your model a local name, e.g., `gpt-4o`. This is the name you will use to call it.
*   **Provider:** Select the cloud service, e.g., "OpenAI (ChatGPT)".
*   **API Key:** Paste your API key from the provider. It will be stored securely.
4.  Click `Save`. The plugin will configure Ollama in the background.

### Step 3: Use Your Custom Model in the AI Assistant
You can now chat with your new model directly in the AI Assistant.

1.  Open the AI Assistant chat window.
2.  In the chat input, type `/` or click the model selection button.
3.  Your custom model (e.g., `gpt-4o`) will now appear in the list of available Ollama models.
4.  Select it and start chatting! All requests will be routed through your local Ollama to the cloud provider using your API key.

## Development

Want to contribute? Please feel free to submit a pull request!

### Building from Source

1.  **Prerequisites:** JDK 21 and IntelliJ IDEA 2024.2 or newer.
2.  **Clone:** `git clone https://github.com/intisy/ollama-forward.git`
3.  **Build:** `./gradlew build`
4.  **Run for Testing:** `./gradlew runIde`

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.