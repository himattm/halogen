## 2024-07-14 - Optimize Hex Color Parsing and Formatting in KMP Hot Paths
**Learning:** In Kotlin Multiplatform hot paths (such as hex color serialization and parsing), manual character array manipulation and bitwise shifts are substantially faster (up to 25x faster for serialization and 10x faster for parsing) than using standard library string methods like `toString(16).padStart()`, `uppercase()`, or `substring().toLong(16).toInt()`. This approach avoids platform-specific string allocation overhead.
**Action:** When working in KMP hot paths, avoid standard library string manipulations that instantiate multiple objects per call. Favor `CharArray`, manual index iteration, bitwise shifts, and `concatToString()` to minimize allocations and latency.

## 2026-07-21 - [Precomputing Small Domain Conversions]
**Learning:** [When mapping a strictly bounded 8-bit domain (0-255) like RGB components to a complex function (e.g. gamma expansion / delinearization), calculating floating point operations repeatedly in a hot path causes measurable overhead. Caching results in a DoubleArray(256) reduces execution time by several orders of magnitude without memory bloat.]
**Action:** [Always audit standard math functions on constrained discrete domains (e.g., color, 8-bit audio) for caching opportunities to replace CPU cycles with fast array lookups.]
