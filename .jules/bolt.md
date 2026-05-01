## 2024-05-24 - Regex Backtracking on LLM JSON Outputs
**Learning:** Using regex with `[\s\S]*?` to strip markdown fences from potentially large LLM JSON outputs introduces unnecessary overhead due to engine initialization and character backtracking.
**Action:** When extracting large structured text blocks, replace regex extraction with raw string manipulation (`indexOf`, `substring`) to improve performance and reduce memory allocations. Beware of `lastIndexOf` vs `indexOf(..., startIndex)` to maintain the correct greediness logic when dealing with multiple fenced blocks.
