## 2024-05-24 - Dynamic Regex compilation penalty in Kotlin
**Learning:** Compiling a regular expression dynamically within a function is an expensive operation in Kotlin, and doing so in a function that may be called repeatedly causes a performance penalty.
**Action:** Always hoist frequently used or dynamically compiled  strings to class-level or object-level constants (e.g. ) to optimize performance and prevent repeated recompilation costs.
## 2024-05-24 - Dynamic Regex compilation penalty in Kotlin
**Learning:** Compiling a regular expression dynamically within a function is an expensive operation in Kotlin, and doing so in a function that may be called repeatedly causes a performance penalty.
**Action:** Always hoist frequently used or dynamically compiled Regex strings to class-level or object-level constants to optimize performance and prevent repeated recompilation costs.
