## 2025-02-18 - Avoid catastrophic regex backtracking for Markdown content
**Learning:** Kotlin's Regex using `[\s\S]*?` on large strings (such as multiline LLM JSON output wrapped in Markdown code fences) can trigger catastrophic backtracking, causing severe performance degradation or hangs during schema parsing.
**Action:** Use raw string extraction methods (like `indexOf` and `substring`) instead of regex when stripping code fences or matching large unbounded text blocks.
