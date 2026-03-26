# Custom Extensions

Add your own theme tokens beyond the standard Material 3 palette.

---

## What Are Extensions

Extensions let you define custom color tokens that the LLM will generate alongside the standard M3 theme. Need a `success` color, a `warning` color, or gradient endpoints? Register them as extensions, and the LLM generates values that harmonize with the rest of the theme.

Extensions are returned in the `ext` field of `HalogenThemeSpec`:

```json
{
  "pri": "#1A73E8",
  "sec": "#34A853",
  ...
  "ext": {
    "success": "#2E7D32",
    "warning": "#F9A825",
    "brandGradientStart": "#1A73E8",
    "brandGradientEnd": "#6DD5FA"
  }
}
```

---

## Registering Extensions

Pass extensions to the builder at initialization:

```kotlin
val halogen = Halogen.Builder()
    .provider(GeminiNanoProvider())
    .extensions(
        HalogenExtension("success", "A green-ish color for success states"),
        HalogenExtension("warning", "An amber/yellow color for warnings"),
        HalogenExtension("info", "A blue color for informational states"),
        HalogenExtension("brandGradientStart", "Start color for brand gradient"),
        HalogenExtension("brandGradientEnd", "End color for brand gradient"),
    )
    .cache(HalogenCache.memory()) // or HalogenRoomCache.create() with halogen-cache-room
    .build()
```

Each `HalogenExtension` takes a key (the JSON field name) and a description (used in the prompt to guide the LLM).

---

## How Extensions Work

When extensions are registered, the engine appends them to the LLM prompt:

```
Also include these custom color tokens in an "ext" object:
- "success": A green-ish color for success states
- "warning": An amber/yellow color for warnings
- "info": A blue color for informational states
- "brandGradientStart": Start color for brand gradient
- "brandGradientEnd": End color for brand gradient
```

The LLM sees these descriptions and generates hex color values that match the overall theme. A "neon cyberpunk" theme will produce a neon green `success`, while a "warm cozy" theme will produce an earthy green `success`.

---

## Token Budget

Each extension adds approximately:

- **+15 tokens** to input (the description in the prompt)
- **+10 tokens** to output (the key-value pair in the JSON response)

| Extensions | Input Tokens | Output Tokens | Total Output |
|-----------|-------------|---------------|-------------|
| 0 | ~1,100 | ~70 | ~70 |
| 5 | ~1,175 | ~120 | ~120 |
| 10 | ~1,250 | ~170 | ~170 |

!!! warning "Maximum: 10 extensions"
    To stay safely within Gemini Nano's token limits (256 output tokens), register at most 10 custom extensions. The engine will throw `IllegalArgumentException` if you exceed this limit.

---

## Accessing Extensions in Compose

Read extension values inside any composable wrapped by `HalogenTheme`:

```kotlin
@Composable
fun SuccessBanner(message: String) {
    val successColor = HalogenTheme.extensions["success"]?.toColor()
        ?: Color(0xFF2E7D32)  // fallback if extension is missing

    Surface(color = successColor) {
        Text(message, color = Color.White)
    }
}
```

Extensions are provided via `LocalHalogenExtensions`:

```kotlin
@Composable
fun GradientHeader() {
    val extensions = HalogenTheme.extensions
    val start = extensions["brandGradientStart"]?.toColor() ?: MaterialTheme.colorScheme.primary
    val end = extensions["brandGradientEnd"]?.toColor() ?: MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Brush.horizontalGradient(listOf(start, end)))
    )
}
```

!!! tip "Always provide fallbacks"
    Extensions depend on LLM output and may be missing if the LLM omits a field or if the theme was loaded from a server that doesn't include extensions. Always provide a sensible default color.
