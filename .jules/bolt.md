## 2025-03-09 - Markdown Code Fence Parsing Performance

**Learning:** When parsing large structures like LLM outputs or Markdown code fences, using regex with backtracking constructs (like `[\s\S]*?`) introduces significant performance overhead compared to raw string searching. This can block the main thread or degrade server performance when handling large or heavily-nested payloads.
**Action:** Replace backtracking regex patterns with manual string extraction using standard library functions like `indexOf`, `regionMatches`, and `substring`. While a bit more verbose, this scales significantly better and is much faster for parsing repetitive or large string bodies.
