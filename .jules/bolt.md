## 2024-05-18 - Replacing greedy Regex with substring extraction
**Learning:** Regexes utilizing `[\s\S]*?` for parsing large structures (like LLM outputs or Markdown code fences) introduce significant performance overhead due to backtracking.
**Action:** Use raw string extraction methods (`indexOf`, `substring`) instead. To correctly replicate lazy (non-greedy) matching, use `indexOf(delimiter, startIndex)` to find the next occurrence rather than `lastIndexOf`, which acts greedily and will incorrectly capture content across multiple distinct blocks.
