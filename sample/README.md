# Halogen Playground

Interactive demo app for the [Halogen](https://github.com/himattm/halogen) library. Generate Material 3 themes from natural language prompts and tune LLM parameters in real time.

## Features

- **Playground** — Type a prompt, adjust temperature/topK/topP, generate a theme, see every M3 component update live
- **Weather** — 8 weather conditions, each auto-generating a contextual theme
- **Test Harness** — Matrix view: run prompts against multiple config presets, compare results
- **Settings** — Gemini Nano status, model download/warmup, manage cache

## Running

1. Open in Android Studio
2. Select the `sample` run configuration
3. Run on a device or emulator

## On-Device LLM

This sample uses Gemini Nano via ML Kit for on-device theme generation. Requires a supported device (Pixel 9+, Samsung S25+) with a locked bootloader. On unsupported devices, the app falls back to the default Material You theme.

For the cross-platform sample with cloud LLM support (OpenAI via Ktor), see the `sample-shared` module.
