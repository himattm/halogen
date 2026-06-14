## 2024-06-14 - [Kotlin String Parsing Optimization]
**Learning:** In Kotlin hot paths (e.g., hex color conversions and formatting), manual character array manipulation and bitwise shifts are 4-5x faster than using standard library string methods like `toString(16).padStart()`, `uppercase()`, or `substring().toLong(16).toInt()` because they avoid allocating intermediate `String` objects.
**Action:** When working on performance optimizations in hot paths, avoid using `substring`, `toLong`, and `padStart` on Strings, and instead use manual looping and bit shifting.
