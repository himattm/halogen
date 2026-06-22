## 2024-06-22 - Optimize ThemeExpander hot path hex operations
**Learning:** In Kotlin Multiplatform hot paths (like ThemeExpander.kt which might be called frequently during theming calculations), string manipulations like `substring(1).toLong(16).toInt()` or `toString(16).padStart(6, '0').uppercase()` cause unnecessary object allocations and performance overhead.
**Action:** Replace these operations with manual character parsing and bitwise shifts. For string building, use `CharArray` and `concatToString()` instead of `StringBuilder` or standard library string formatters.
