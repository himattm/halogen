## 2025-02-18 - KMP WasmJs JSON Decoding Overhead
**Learning:** JSON decoding in Kotlin Multiplatform for WasmJs targets carries significant overhead. Repeatedly parsing the `localStorage` manifest JSON string in `LocalStorageThemeCache.kt` on every read/write was causing unnecessary bottlenecks.
**Action:** Use an in-memory cache initialized via `by lazy` to parse the `localStorage` JSON only once upon the first access, and perform subsequent reads/writes directly from/to this in-memory cache to avoid redundant parsing.
