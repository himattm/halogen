package halogen.engine

import halogen.HalogenThemeSpec

/**
 * Persists and restores the last active [HalogenThemeSpec].
 *
 * This is an internal implementation detail of [HalogenEngine] — developers never
 * interact with it directly. The engine saves the active theme on every apply and
 * restores it automatically on construction.
 *
 * Platform implementations:
 * - Android, JVM, iOS: DataStore Preferences
 * - WasmJs: browser localStorage
 */
internal interface ThemePersistence {
    suspend fun save(spec: HalogenThemeSpec)
    suspend fun load(): HalogenThemeSpec?

    /**
     * Synchronous initial load for use in the engine constructor,
     * before any coroutine scope is available. Eliminates the flash
     * from default theme → persisted theme on app startup.
     *
     * Platform implementations:
     * - Android/JVM/iOS: `runBlocking { load() }`
     * - WasmJs: direct localStorage read (already synchronous)
     */
    fun loadInitial(): HalogenThemeSpec?
}

/** Platform-specific factory for the default [ThemePersistence]. */
internal expect fun createDefaultPersistence(): ThemePersistence
