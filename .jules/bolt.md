## 2024-05-24 - WasmJs localStorage JSON parsing overhead
**Learning:** JSON decoding in Kotlin Multiplatform for WasmJs targets carries significant overhead. Parsing the same JSON manifest from localStorage repeatedly causes a measurable performance bottleneck.
**Action:** Use an in-memory cache initialized via `by lazy` to avoid redundant localStorage parsing for frequently accessed metadata like manifests.
