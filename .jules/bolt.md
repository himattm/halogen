## 2025-05-18 - Optimized HintExtractor
**Learning:** Replaced multiple Regex compilations and operations in `HintExtractor.extract` with a single manual character iteration loop using `StringBuilder`. This prevents significant compilation and backtracking overhead in Kotlin hot paths. The environment has a broken `halogen-cache-room` configuration (missing androidTarget) which prevents Gradle tasks from succeeding, but this was a pre-existing issue.
**Action:** Always prefer manual string iterations over complex regex pipelines in Kotlin when parsing strings in performance-critical paths.
