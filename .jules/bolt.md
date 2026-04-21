## 2025-04-20 - [LocalStorageThemeCache caching bottleneck]
**Learning:** The `LocalStorageThemeCache` does redundant JSON decoding of the entire manifest on every operation (read/write/evict/contains) which creates overhead for wasmJs.
**Action:** Introduce an in-memory Set to cache the manifest, updating it during mutations and lazy-loading it once. This avoids repeated JSON decoding on `readManifest()`.
