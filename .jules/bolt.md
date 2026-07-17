## 2024-07-17 - Manual string manipulation in KMP hot paths
**Learning:** In Kotlin Multiplatform hot paths (like hex color parsing/serialization), standard library string operations (like `toString(16).padStart()`, `.uppercase()`, or `.substring(1).toLong(16).toInt()`) incur significant performance overhead due to allocations and regex usage under the hood. Manual character iteration and bitwise operations are significantly faster.
**Action:** Replace standard library string manipulation with manual char iteration loops for fixed-length formatting tasks (like hex colors) in KMP hot paths.

## 2024-07-17 - Avoid regex in tests where possible
**Learning:** Initializing `Regex` objects, even in tests, has a noticeable overhead. Simple fixed-pattern checks (like hex code validation `^#[0-9A-Fa-f]{6}$`) can be implemented with a manual loop for a small speed up.
**Action:** Replace simple validation regexes with manual character check functions where appropriate to eliminate regex compilation overhead.
