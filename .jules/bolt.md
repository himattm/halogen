## 2024-07-14 - Optimize Hex Color Parsing and Formatting in KMP Hot Paths
**Learning:** In Kotlin Multiplatform hot paths (such as hex color serialization and parsing), manual character array manipulation and bitwise shifts are substantially faster (up to 25x faster for serialization and 10x faster for parsing) than using standard library string methods like `toString(16).padStart()`, `uppercase()`, or `substring().toLong(16).toInt()`. This approach avoids platform-specific string allocation overhead.
**Action:** When working in KMP hot paths, avoid standard library string manipulations that instantiate multiple objects per call. Favor `CharArray`, manual index iteration, bitwise shifts, and `concatToString()` to minimize allocations and latency.

## 2024-07-15 - Precompute sRGB to Linear LUT in Color Math
**Learning:** In KMP hot paths for color manipulation, mathematical operations converting 8-bit color components (0-255) to linear space perform redundant calculations like `.pow()` and division.
**Action:** Use a pre-computed `DoubleArray(256)` lookup table for operations that strictly map a discrete 8-bit domain to improve performance.
