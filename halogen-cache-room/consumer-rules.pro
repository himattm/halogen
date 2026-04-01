# Halogen Cache Room — Consumer ProGuard Rules

# --- Public API ---
-keep class halogen.cache.room.HalogenRoomCache { *; }
-keep class halogen.cache.room.RoomThemeCacheConfig { *; }
-keep class halogen.cache.room.RoomThemeCache { *; }

# --- Room database internals (needed at runtime) ---
-keep class halogen.cache.room.db.** { *; }
