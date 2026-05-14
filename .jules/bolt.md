## 2024-05-14 - Hoist dynamic Regex objects to class-level constants
**Learning:** Declaring Regex objects dynamically within functions incurs a performance penalty due to recompilation on every invocation.
**Action:** Always hoist frequently used regexes (e.g., Regex("""\s+""")) to class-level or object-level constants (e.g., private val) to optimize performance in hot paths.
