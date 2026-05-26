## 2024-06-25 - Replace regex with character iteration in hot path
**Learning:** Replacing lookaround and formatting regexes with a single manual character iteration loop using a `StringBuilder` significantly reduces compilation overhead, object allocation, and backtracking delays in hot paths for string processing.
**Action:** Always consider converting complex, heavily-used `Regex` replacements into manual string parsing functions when performance is a priority and the processing logic involves straightforward conditions like prefix checking, whitespace handling, and character cases.
