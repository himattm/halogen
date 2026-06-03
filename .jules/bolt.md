
## 2024-05-24 - Single Pass Loop Over Multiple Regexes
**Learning:** In Kotlin hot paths, relying on multiple regular expressions (e.g., lookarounds for camel case, format validation, whitespace normalization) introduces significant overhead due to state machine compilation and backtracking.
**Action:** When extracting strings or validating common patterns in hot paths, replace multiple Regex instances with a single manual character iteration loop and a `StringBuilder` to achieve measurable performance gains and avoid backtracking overhead.
