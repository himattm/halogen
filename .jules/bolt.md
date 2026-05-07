## 2024-05-24 - WasmJs JSON Parsing Overhead
**Learning:** JSON decoding in Kotlin Multiplatform for WasmJs targets carries significant overhead, making repeated parsing of localStorage contents a performance bottleneck.
**Action:** Use an in-memory cache (e.g., via `by lazy`) to store parsed JSON structures and only sync to localStorage when necessary.
