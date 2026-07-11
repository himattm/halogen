
## 2024-07-11 - Optimize Kotlin hex color parsing & serialization
**Learning:** In Kotlin Multiplatform hot paths (such as hex color serialization and parsing), manual character array manipulation and bitwise shifts are substantially faster (up to 25x faster for serialization and 10x faster for parsing) than using standard library string methods like `toString(16).padStart()`, `uppercase()`, or `substring().toLong(16).toInt()`. This approach avoids platform-specific string allocation overhead.
**Action:** Use manual loops and bitwise operations instead of string allocation and format functions for critical hex color math.
