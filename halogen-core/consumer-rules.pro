# Halogen Core — Consumer ProGuard Rules
# These rules are automatically applied to any app that depends on halogen-core.

# --- Public API: theme spec & config ---
-keep class halogen.HalogenThemeSpec { *; }
-keep class halogen.HalogenThemeSpec$Companion { *; }
-keep class halogen.HalogenThemeSpec$$serializer { *; }
-keep class halogen.HalogenConfig { *; }
-keep class halogen.HalogenConfig$Companion { *; }
-keep class halogen.HalogenDefaults { *; }

# --- Public API: LLM provider contract ---
-keep class halogen.HalogenLlmProvider { *; }
-keep class halogen.HalogenLlmAvailability { *; }
-keep class halogen.HalogenLlmException { *; }

# --- Public API: extensions ---
-keep class halogen.HalogenExtension { *; }
-keep class halogen.HalogenExtensions { *; }

# --- Public API: theme expansion ---
-keep class halogen.ThemeExpander { *; }
-keep class halogen.ExpandedTheme { *; }
-keep class halogen.HalogenPalette { *; }
-keep class halogen.HalogenColorScheme { *; }
-keep class halogen.HalogenTypography { *; }
-keep class halogen.HalogenShapes { *; }

# --- Public API: prompt & schema ---
-keep class halogen.PromptBuilder { *; }
-keep class halogen.SchemaParser { *; }

# --- Public color science classes ---
-keep class halogen.color.Hct { *; }
-keep class halogen.color.TonalPalette { *; }

# --- kotlinx-serialization support ---
-keepclassmembers class halogen.** {
    *** Companion;
}
-keepclasseswithmembers class halogen.** {
    kotlinx.serialization.KSerializer serializer(...);
}
