# Halogen Cast Demo — Sender

Android reference source that exercises `halogen-chromecast` end-to-end with
five hand-picked palettes. Tapping a swatch applies the theme locally and
(when a Cast session is active) migrates it to the receiver.

## How to use this sample

This directory is **reference code**, not a registered Gradle project in this
repo (to avoid requiring every CI run to resolve the Google Cast SDK). To run
it:

1. Create a new Android app module in your own project.
2. Copy the Kotlin sources from `src/main/kotlin/` and the manifest from
   `src/main/AndroidManifest.xml` into it.
3. Add these dependencies:

   ```kotlin
   implementation("me.mmckenna.halogen:halogen-core:0.2.0")
   implementation("me.mmckenna.halogen:halogen-engine:0.2.0")
   implementation("me.mmckenna.halogen:halogen-chromecast:0.2.0")
   implementation("com.google.android.gms:play-services-cast-framework:21.4.0")
   implementation("androidx.mediarouter:mediarouter:1.7.0")
   // + Compose BOM / material3 / activity-compose / lifecycle-runtime-ktx
   ```

4. Register a receiver application in the
   [Google Cast SDK Developer Console](https://cast.google.com/publish/) and
   point your `CastOptionsProvider` at the App ID.
5. Host the companion `sample-chromecast-receiver/` static site over HTTPS
   (e.g. GitHub Pages, Firebase Hosting) and set it as your receiver's URL.

## What the app shows

- Grid of 5 named palettes. Tap one to `engine.apply(...)` it.
- The `handoffToChromecast` extension forwards every `activeTheme` change on
  `autoSend = true`; no explicit send call is needed.
- A one-shot "Theme sent to *device*" pulse toast appears whenever
  `HandoffState` transitions to `Acknowledged`.

The Compose toast implementation lives in `HandoffToast.kt` — it's intentionally
app-owned, not library-owned, so consumers can style it to match their brand.
