## 2025-02-28 - Fast Hex Color Conversions
**Learning:** In Kotlin hot paths (e.g., hex color conversions and formatting), manual character array manipulation and bitwise shifts are 10-20x faster than using standard library string methods like `toString(16).padStart()`, `uppercase()`, or `substring().toLong(16).toInt()` because they avoid allocating intermediate `String` objects.
**Action:** Replace high-frequency `argbToHex` and `parseHexToArgb` in `ThemeExpander.kt` with manual bitwise operations and character arrays to save overhead on every theme color generated.
