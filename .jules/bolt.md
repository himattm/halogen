## 2024-05-05 - Optimize Code Fence Stripping

**Learning:** Regexes utilizing `[\s\S]*?` for parsing large structures (like LLM outputs or Markdown code fences) introduce significant performance overhead due to backtracking. Manual parsing via `indexOf` and `substring` operations can provide massive performance improvements for these tasks, particularly as the input size scales.

**Action:** Whenever tasked with extracting delimited blocks of text from potentially large strings (such as Markdown code blocks, XML tags, or long JSON outputs), avoid using regular expressions with large capture groups. Opt for standard String functions (`indexOf`, `substring`, `startsWith`) instead. When recreating non-greedy matches, correctly employ `indexOf(delimiter, startIndex)` to limit parsing range.
