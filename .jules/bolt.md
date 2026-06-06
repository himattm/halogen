## 2025-02-18 - Manual String Builder for Extractors
**Learning:** Sequential Regex modifications in hot path extractors (like `HintExtractor.kt`) cause massive overhead due to state machine initialization, capture groups, and temporary object creation.
**Action:** Replace sequential `Regex.replace` and `Regex.matches` with a single-pass loop over the character array using a `StringBuilder` pre-sized to `cleaned.length + n`. Utilize early exits with `substring` over regex prefix removals. This reduced execution time by roughly 80%.

## 2025-02-18 - Gradle Test OOM via memory threshold
**Learning:** Running `:test` sequentially inside tight environment limits via Gradle wrapper sometimes throws OOM or kills tasks randomly.
**Action:** Use `-Pkotlin.compiler.execution.strategy=in-process` alongside standard Gradle wrapper tasks when testing JVM limits to run compile in process and not spin up external daemons that steal process memory.
