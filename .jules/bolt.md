## 2024-04-22 - WasmJs JSON Decoding Overhead
**Learning:** JSON decoding in Kotlin Multiplatform for WasmJs targets carries significant overhead when accessing localStorage. Re-parsing the same JSON structure repeatedly (e.g., manifest keys) negatively impacts performance on Web.
**Action:** Use an in-memory cache initialized when needed (or standard memory properties) to keep a parsed copy of frequently accessed data, thus minimizing redundant `localStorage` operations and `kotlinx.serialization` parsing penalties on WasmJs targets.
