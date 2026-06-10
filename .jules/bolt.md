## 2024-06-10 - Hex color conversions
**Learning:** In Kotlin hot paths (e.g., hex color conversions and formatting), manual character array manipulation and bitwise shifts are 4-40x faster than using standard library string methods like `toString(16).padStart()`, `uppercase()`, or `substring().toLong(16).toInt()` because they avoid allocating intermediate `String` objects.
**Action:** When finding bottlenecks in string parsing or formatting (e.g. for hex colors), check if manual char-by-char bitwise math or char array assignments can be used to skip allocations.
