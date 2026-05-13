
## 2024-05-18 - Hoist Regex in hot paths
**Learning:** In Kotlin, creating `Regex` objects inside functions dynamically incurs a performance penalty because the regex is recompiled on each invocation. This is especially true for often-called utility functions (like normalizing strings or extracting hints).
**Action:** Always hoist regular expressions (e.g., `Regex("""\s+""")`) to private or public `val` constants at the class/object level to execute compilation only once.
