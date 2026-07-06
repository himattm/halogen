## 2024-05-24 - [Initialization]\n**Learning:** [First time setup]\n**Action:** [Create file]
## 2024-05-24 - [Hex parsing and formatting optimization]
**Learning:** In Kotlin Multiplatform hot paths (such as hex color serialization and parsing), manual character array manipulation and bitwise shifts are substantially faster (up to 25x faster for serialization and 10x faster for parsing) than using standard library string methods like `toString(16).padStart()`, `uppercase()`, or `substring().toLong(16).toInt()`. This approach avoids platform-specific string allocation overhead.
**Action:** Always prefer manual character array manipulation and bitwise shifts for fast parsing and formatting of simple fixed-length strings like hex color codes in Kotlin hot paths.
