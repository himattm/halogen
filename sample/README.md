# Halogen Sample App

A reference Android application demonstrating Halogen's key features.

## Features Demonstrated

- Theme generation from natural language prompts
- Config preset switching (Default, Vibrant, Muted, etc.)
- Custom extensions (success, warning, brand gradient)
- Provider chaining (Gemini Nano + OpenAI cloud fallback)
- Cache management (evict, refresh, inspect)
- Light/dark mode instant switching

## Running

1. Open the project root in Android Studio.
2. Select the `sample` run configuration.
3. Run on an Android device or emulator (API 26+).

### Cloud Provider Setup (Optional)

To test the OpenAI cloud fallback, add your API key to `local.properties`:

```properties
OPENAI_API_KEY=sk-your-key-here
```

The app works without this -- it will use Gemini Nano if available, or show the default theme.
