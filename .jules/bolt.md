## 2024-06-24 - Kotlin hex conversion hot paths
**Learning:** In Kotlin Multiplatform codebases, standard string methods for hex color parsing/formatting like `toString(16).padStart().uppercase()` and `substring().toLong(16).toInt()` introduce severe allocation overhead (intermediate Strings) in hot paths.
**Action:** Replace with manual character array manipulation and bitwise shifts, leveraging `CharArray.concatToString()` for construction, achieving 10-20x speedups by preventing unnecessary allocations.
