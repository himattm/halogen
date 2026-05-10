## 2026-05-10 - Extract Dynamic Regex Declarations
**Learning:** In Kotlin, declaring `Regex` objects dynamically within functions incurs a performance penalty due to recompilation on every invocation.
**Action:** Always hoist frequently used regexes (e.g., `Regex("""\s+""")`) to class-level or object-level constants (e.g., `private val`) to optimize performance in hot paths.
