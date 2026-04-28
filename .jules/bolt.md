## 2024-04-28 - Avoid Greedy Multiline Regex for String Parsing
**Learning:** Regexes utilizing `[\s\S]*?` for parsing large multiline strings (like LLM outputs or Markdown code fences) introduce significant performance overhead due to backtracking. In JVM environments, this can severely bottleneck execution.
**Action:** Replace multi-line greedy regex extractions with raw string scanning methods (like `indexOf` and `substring`), ensuring correct implementation of optional boundaries while avoiding engine backtracking.
