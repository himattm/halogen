# Custom Theme Systems

Use Halogen's LLM-generated themes with any design system — not just Material 3.

---

## Architecture: M3-Agnostic Core

Halogen's core is intentionally decoupled from Material 3. The `halogen-core` module outputs:

- **`HalogenColorScheme`** — 49 color roles as ARGB integers
- **`HalogenTypography`** — font mood, heading/body weights, letter spacing
- **`HalogenShapes`** — 5 corner radius sizes in dp
- **`ExpandedTheme`** — bundles all of the above (light + dark schemes)

None of these types reference Material 3. The M3 integration lives entirely in `halogen-compose`, which is optional.

---

## Direct Usage with ThemeExpander

If you only need the expanded theme data (no Compose UI):

```kotlin
val spec: HalogenThemeSpec = ... // from LLM, cache, or server
val expanded = ThemeExpander.expand(spec, HalogenConfig.Default)

// Access raw color values (ARGB ints)
val primaryColor = expanded.lightColorScheme.primary       // e.g., 0xFF1A73E8
val darkSurface = expanded.darkColorScheme.surface          // e.g., 0xFF1C1B1F

// Access typography
val fontHint = expanded.typography.fontFamilyHint()         // e.g., "sans-serif"
val headingWeight = expanded.typography.headingWeight        // e.g., 700

// Access shapes
val cornerRadius = expanded.shapes.medium                   // e.g., 16.0f dp
```

---

## Mapping to a Custom Theme

Here's a concrete example mapping Halogen's output to a hypothetical company design system:

```kotlin
// Your company's theme data class
data class AcmeTheme(
    val brandPrimary: Color,
    val brandSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val backgroundMain: Color,
    val backgroundCard: Color,
    val borderDefault: Color,
    val cornerRadius: Dp,
    val headingFontWeight: FontWeight,
    val bodyFontWeight: FontWeight,
)

// Map ExpandedTheme to your theme
fun ExpandedTheme.toAcmeTheme(isDark: Boolean): AcmeTheme {
    val colors = if (isDark) darkColorScheme else lightColorScheme
    return AcmeTheme(
        brandPrimary = Color(colors.primary),
        brandSecondary = Color(colors.secondary),
        textPrimary = Color(colors.onSurface),
        textSecondary = Color(colors.onSurfaceVariant),
        backgroundMain = Color(colors.surface),
        backgroundCard = Color(colors.surfaceContainer),
        borderDefault = Color(colors.outlineVariant),
        cornerRadius = shapes.medium.dp,
        headingFontWeight = FontWeight(typography.headingWeight),
        bodyFontWeight = FontWeight(typography.bodyWeight),
    )
}
```

---

## Using HalogenTheme with a Custom Wrapper

`HalogenTheme` supports a `themeWrapper` parameter that lets you replace the default `MaterialTheme` wrapping with your own:

```kotlin
// Composition local for your custom theme
val LocalAcmeTheme = staticCompositionLocalOf { AcmeTheme.defaults() }

HalogenTheme(
    spec = currentSpec,
    themeWrapper = { expanded, isDark, content ->
        val acmeTheme = remember(expanded, isDark) {
            expanded.toAcmeTheme(isDark)
        }
        CompositionLocalProvider(
            LocalAcmeTheme provides acmeTheme,
        ) {
            content()
        }
    },
) {
    // Inside here, access your theme:
    val theme = LocalAcmeTheme.current
    Text("Hello", color = theme.textPrimary)
}
```

When `themeWrapper` is provided, `HalogenTheme` handles the spec expansion and dark mode switching, but delegates the actual theme application to your wrapper. No `MaterialTheme` is applied.

---

## Extensions with Custom Themes

Custom extensions work the same way regardless of whether you use Material 3 or a custom theme system. Register them via the engine builder:

```kotlin
val halogen = Halogen.Builder()
    .provider(myProvider)
    .extensions(
        HalogenExtension("brandAccent", "A vibrant accent for your brand"),
        HalogenExtension("successGreen", "Success state color"),
    )
    .build()
```

Access them via `HalogenTheme.extensions`:

```kotlin
val accent = HalogenTheme.extensions["brandAccent"]?.toColor()
    ?: fallbackColor
```

---

## Which Modules Do You Need?

| Use Case | Modules |
|----------|---------|
| Material 3 theme (standard) | `halogen-core` + `halogen-engine` + `halogen-compose` |
| Custom theme system in Compose | `halogen-core` + `halogen-engine` + `halogen-compose` |
| Non-Compose (raw theme data) | `halogen-core` + `halogen-engine` |
| Color science only | `halogen-core` |

`halogen-compose` is needed even for custom themes if you want the `HalogenTheme` composable with `themeWrapper`. If you're building outside Compose entirely, use `halogen-core` + `halogen-engine` and consume `ExpandedTheme` directly.
