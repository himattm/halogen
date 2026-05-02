## 2024-06-25 - Regex Backtracking on LLM Output
**Learning:** Using `[\s\S]*?` in regular expressions to extract markdown code fences from large LLM JSON outputs can cause severe backtracking and performance overhead. The regex engine evaluates character by character, leading to O(N) or even worse degradation depending on engine implementation and input size.
**Action:** When extracting chunks (like code fences) from large blocks of text, bypass regex entirely and use explicit string operations (`indexOf`, `substring`). Use `indexOf` with a `startIndex` parameter to safely emulate lazy matching.

## 2024-06-25 - NullPointerException in Compose Tests
**Learning:** When tests in `halogen-compose` fail with a `NullPointerException` referencing `android.os.Build.FINGERPRINT` while calling Compose UI functions like `lightColorScheme().primary`, it is a known environmental constraint. The Robolectric test environment used for Compose UI tests on Android sometimes isn't initialized correctly or mocks `Build.FINGERPRINT` as null, leading to this failure. It is NOT a bug in the codebase's logic.
**Action:** Recognize this specific stack trace as an environmental Compose UI testing constraint. Do not attempt to refactor the production Compose code to work around it. Use `knowledgebase_lookup` to confirm this behavior when encountering similar test failures.
