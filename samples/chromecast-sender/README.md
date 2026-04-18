# Halogen Cast Demo — Sender

Android app that exercises `halogen-chromecast` end-to-end with five hand-picked
palettes. Tapping a swatch applies the theme locally and (when a Cast session is
active) migrates it to the receiver.

## Build

```
./gradlew :samples:chromecast-sender:installDebug
```

## Wire up your Cast receiver

The sample ships with Google's default media receiver (`CC1AD845`) as a
placeholder — it will accept the connection but won't render the Halogen
palette. To see the full palette end-to-end:

1. Register a receiver application in the
   [Google Cast SDK Developer Console](https://cast.google.com/publish/).
2. Host the `samples/chromecast-receiver/` static site over HTTPS (e.g.
   GitHub Pages, Firebase Hosting) and set that URL as your receiver's URL.
3. Rebuild with the App ID wired in:

   ```
   ./gradlew :samples:chromecast-sender:installDebug \
       -PHALOGEN_CAST_APP_ID=ABCDEF12
   ```

   Or export `HALOGEN_CAST_APP_ID` as an environment variable.

## What the app shows

- Grid of 5 named palettes. Tap one to `engine.apply(...)` it.
- The `handoffToChromecast` extension forwards every `activeTheme` change on
  `autoSend = true`; no explicit send call is needed.
- A one-shot "Now playing on *device*" pulse toast appears whenever
  `HandoffState` transitions to `Acknowledged`.

The Compose toast implementation lives in `HandoffToast.kt` — it's intentionally
app-owned, not library-owned, so consumers can style it to match their brand.
