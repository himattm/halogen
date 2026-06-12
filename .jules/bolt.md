## 2024-05-18 - Hex Color Parsing/Formatting Avoids String Allocations
**Learning:** In Kotlin hot paths (e.g., hex color conversions and formatting), manual character array manipulation and bitwise shifts are 4-5x faster than using standard library string methods like `toString(16).padStart()`, `uppercase()`, or `substring().toLong(16).toInt()` because they avoid allocating intermediate `String` objects.
**Action:** When optimizing color processing logic that relies heavily on hex parsing/formatting, replace string manipulation with explicit loops over `CharArray` and bitwise operations.
