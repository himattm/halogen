# Chromecast

`halogen-chromecast` is a lightweight KMP module that migrates the current
Halogen palette from a sender device (phone, tablet) to a Chromecast receiver
so the TV renders the same theme.

The module is intentionally Cast-SDK-free. It defines the wire protocol, state
machine, and engine integration; consumers adapt their Cast session (Android
`play-services-cast`, iOS `google-cast-sdk`) to a small `CastMessageTransport`
interface.

## Install

```kotlin
implementation("me.mmckenna.halogen:halogen-chromecast:0.2.0")
```

Depends on `halogen-core` and `halogen-engine`. Multiplatform: Android, iOS,
JVM, WasmJs.

## Five-minute setup

```kotlin
class MyActivity : ComponentActivity() {
    private lateinit var transport: CastTransportAndroid
    private lateinit var handoff: HalogenCastSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val castContext = CastContext.getSharedInstance(this)
        transport = CastTransportAndroid(castContext)
        handoff = engine.handoffToChromecast(
            transport = transport,
            scope = lifecycleScope,
            senderInfo = SenderInfo(
                appId = packageName,
                appVersion = BuildConfig.VERSION_NAME,
                platform = "android",
                halogenVersion = "0.2.0",
            ),
            autoSend = true,  // forwards every engine.activeTheme change
        )
    }

    override fun onStart() { super.onStart(); transport.install() }
    override fun onStop() { transport.uninstall(); super.onStop() }
    override fun onDestroy() { handoff.close(); super.onDestroy() }
}
```

`handoff.state` is a `StateFlow<HandoffState>` you can observe in Compose to
render the "Now playing on *device*" confirmation moment.

## Namespace

Default namespace is `urn:x-cast:me.mmckenna.halogen`. Any Halogen sender can
talk to any Halogen-aware receiver on this channel — the Cast receiver App ID
is a separate concern. Pass a custom namespace to
`handoffToChromecast(namespace = "urn:x-cast:com.example.mine.halogen")` if you
need isolation.

## Wire format

All messages are JSON with a `"type"` discriminator. Only `key` and `primary`
are required on a theme; every other palette and hint field is nullable so
senders may ship a partial palette.

```json
{
  "type": "theme",
  "v": 1,
  "id": "01HBZ…",
  "ts": 1713412800000,
  "key": "track:123",
  "pri": "#E53935",
  "sec": "#FFB300",
  "label": "Never Gonna Give You Up"
}
```

Minimal viable payload:

```json
{ "type": "theme", "v": 1, "id": "x", "ts": 0, "key": "k", "pri": "#FF0000" }
```

See the `halogen.chromecast.CastMessage` KDoc for the full schema of every
message kind.

## Receiver contract

A Halogen-aware CAF receiver must:

1. Register a custom message bus on `urn:x-cast:me.mmckenna.halogen`.
2. Reject messages where `v > RECEIVER_MAX_V` by replying with
   `ack { ok: false, error: "unsupported_version" }`.
3. On `handshake` → reply with an `ack`.
4. On `theme` → apply whichever color fields are present. When `sec` / `ter` /
   neutrals are missing, derive them from `pri` via M3 tonal-palette rotation.
5. On `clear` → revert to the receiver's default palette.

A reference implementation that expands the compact spec into the full 49-role
M3 palette and renders it on-screen lives in
[`samples/chromecast-receiver`](https://github.com/himattm/halogen/tree/main/samples/chromecast-receiver).

## End-to-end demo

See [`samples/chromecast-sender`](https://github.com/himattm/halogen/tree/main/samples/chromecast-sender)
for an Android app with five hand-picked palettes wired into a real
`CastContext`, and
[`samples/chromecast-receiver`](https://github.com/himattm/halogen/tree/main/samples/chromecast-receiver)
for the companion web receiver.
