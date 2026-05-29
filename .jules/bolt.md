## 2025-05-18 - Optimized HintExtractor
**Learning:** Replaced multiple Regex compilations and operations in `HintExtractor.extract` with a single manual character iteration loop using `StringBuilder`. This prevents significant compilation and backtracking overhead in Kotlin hot paths.
**Action:** Always prefer manual string iterations over complex regex pipelines in Kotlin when parsing strings in performance-critical paths.
