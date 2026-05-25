## 2025-02-25 - Regex overhead in hot paths
**Learning:** Replaced multiple regular expressions in `HintExtractor.kt` with a single manual character iteration loop and a `StringBuilder`. This avoids significant compilation and backtracking overhead, yielding measurable performance gains (from ~1000ms to ~100ms for 50k iterations).
**Action:** Prefer raw string operations and `StringBuilder` loops over complex `Regex` instances for simple string parsing and normalization tasks in frequently executed code paths.
