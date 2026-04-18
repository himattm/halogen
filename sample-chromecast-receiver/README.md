# Halogen Cast Demo — Receiver

Static CAF receiver that renders the **full 49-role Material 3 palette** plus a
sample UI strip for whatever Halogen theme the sender casts. Intended as a
reference implementation for anyone building a Halogen-aware Chromecast app.

## Build

```
npm install
npm run build
```

Output lives in `dist/`. Host over HTTPS.

## Wire it up

1. Register a receiver app in the
   [Google Cast SDK Developer Console](https://cast.google.com/publish/).
2. Upload `dist/` to any HTTPS host (GitHub Pages, Firebase Hosting, Netlify, etc.).
3. Point your Cast app's **Receiver Application URL** at the hosted `index.html`.
4. Build the sender demo with `-PHALOGEN_CAST_APP_ID=<your-id>`.

## Protocol

- Namespace: `urn:x-cast:me.mmckenna.halogen`
- Accepts `handshake`, `theme`, and `clear` messages
- Replies to every message with an `ack` envelope

See `halogen-chromecast` KDoc for full message schemas.

## What the receiver does

- Parses the incoming `ThemePayload`
- Expands the 6-color seed via `@material/material-color-utilities` into ~20
  Material 3 roles for both light and dark schemes
- Paints them onto CSS custom properties (`--halogen-primary`, `--halogen-on-primary`, …)
- Renders a color-role grid (swatches with name + hex) and a sample UI strip
  (headline + body + three button variants + card) using the theme
- Pulses the whole page on every new theme
- Auto-toggles between light and dark every 6 seconds so you can see both
  schemes at a glance

## Dev tips

- When iterating locally, use `npm run watch` plus `npm run serve` (generates a
  self-signed cert) and open the page in the Cast Debug Tool in Chrome.
- The minimal required fields on the wire are `key` + `pri` — the expander
  derives the rest. Try sending `{"type":"theme","v":1,"id":"x","ts":0,"key":"k","pri":"#FF0000"}`
  via the Cast Debug Tool's message harness.
