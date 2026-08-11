## 2024-07-14 - Optimize Hex Color Parsing and Formatting in KMP Hot Paths
**Learning:** In Kotlin Multiplatform hot paths (such as hex color serialization and parsing), manual character array manipulation and bitwise shifts are substantially faster (up to 25x faster for serialization and 10x faster for parsing) than using standard library string methods like `toString(16).padStart()`, `uppercase()`, or `substring().toLong(16).toInt()`. This approach avoids platform-specific string allocation overhead.
**Action:** When working in KMP hot paths, avoid standard library string manipulations that instantiate multiple objects per call. Favor `CharArray`, manual index iteration, bitwise shifts, and `concatToString()` to minimize allocations and latency.

## 2024-05-10 - Optimize Math Operations with Lookup Tables
**Learning:** When mathematical operations depend strictly on a small, discrete domain (e.g., converting 8-bit color components 0..255 from sRGB to linear space), utilizing pre-computed arrays or lookup tables instead of redundant on-the-fly computation (divisions, conditionals, `.pow()`) significantly improves performance in hot paths (over 20x improvement).
**Action:** Identify finite input domains in hot paths and pre-calculate their results into arrays (like `DoubleArray(256)`) instead of doing continuous computations repeatedly.

## 2024-08-11 - Unpack Matrix Elements for KMP Hot Paths
**Learning:** In KMP hot paths (e.g. math operations in color conversion like `matrixMultiply`), explicitly unpacking inner arrays and elements into local variables (like `row[0]`, `matrix[1]`, etc.) avoids multiple array dereferences and bounds checks per operation. This yields a significant performance gain (around 1.5x to 2x speedup).
**Action:** When working with 2D arrays or repeated array index lookups inside a hot loop in KMP, unpack those values into local primitives to avoid JVM/JS bounds checking overheads when flattening the array signature isn't feasible.
