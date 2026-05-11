## 2024-05-11 - Hoisting Regex in Kotlin Hot Paths
**Learning:** Instantiating `Regex` dynamically inside a function block in Kotlin causes the regex to be recompiled on every function call. In hot paths, such as string normalizations (`HintExtractor`), this creates a measurable performance overhead due to unnecessary CPU cycles and garbage collection.
**Action:** Always hoist frequently used regexes to class-level or object-level constants (e.g., `private val`) to compile them once during initialization and optimize performance.
