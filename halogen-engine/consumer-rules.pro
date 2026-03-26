# Halogen Engine — Consumer ProGuard Rules

# --- Entry point & builder ---
-keep class halogen.engine.Halogen { *; }
-keep class halogen.engine.Halogen$Builder { *; }
-keep class halogen.engine.HalogenEngine { *; }

# --- Result hierarchy ---
-keep class halogen.engine.HalogenResult { *; }
-keep class halogen.engine.HalogenResult$* { *; }

# --- Cache abstraction ---
-keep class halogen.engine.ThemeCache { *; }
-keep class halogen.engine.HalogenCache { *; }
-keep class halogen.engine.CacheStats { *; }
-keep class halogen.engine.ThemeCacheEntry { *; }
-keep class halogen.engine.ThemeSource { *; }
-keep class halogen.engine.CacheEvent { *; }
-keep class halogen.engine.CacheEvent$* { *; }

# --- Server provider contract ---
-keep class halogen.engine.HalogenServerProvider { *; }

# --- Theme export (serializable) ---
-keep class halogen.engine.ThemeBundle { *; }
-keep class halogen.engine.ThemeBundle$$serializer { *; }

# --- kotlinx-serialization support ---
-keepclassmembers class halogen.engine.** {
    *** Companion;
}
-keepclasseswithmembers class halogen.engine.** {
    kotlinx.serialization.KSerializer serializer(...);
}
