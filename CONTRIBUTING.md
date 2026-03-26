# Contributing to Halogen

Welcome, and thanks for your interest in contributing to Halogen! Whether you're fixing a bug, adding a feature, improving documentation, or reporting an issue, your help is appreciated.

## Development Setup

### Prerequisites

- **JDK 17** or later
- **Android SDK** (for the sample app and Android-specific modules)
- A recent version of **Android Studio** or **IntelliJ IDEA**

### Getting Started

1. Fork the repository on GitHub.
2. Clone your fork:
   ```bash
   git clone https://github.com/<your-username>/halogen.git
   cd halogen
   ```
3. Verify your setup by running the core tests:
   ```bash
   ./gradlew -q --console=plain :halogen-core:allTests
   ```
   All tests should pass before you start making changes.

## Code Style

- Follow [Kotlin official coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- All library modules enforce `explicitApi()`. Every public declaration must have explicit visibility modifiers and return types.
- All public API must include **KDoc documentation**. Explain what the function/class does, its parameters, return values, and any exceptions it throws.
- Keep imports organized and remove unused ones.

## Testing

- **Write tests** for any new features or bug fixes.
- Run the full test suite before submitting a pull request:
  ```bash
  ./gradlew -q --console=plain :halogen-core:allTests :halogen-engine:jvmTest :halogen-compose:jvmTest
  ```
- Tests live alongside the module they cover, in the appropriate source set (`commonTest`, `jvmTest`, etc.).
- Aim for deterministic tests. Avoid flaky timing-dependent assertions.

## API Compatibility

Halogen uses [Binary Compatibility Validator](https://github.com/Kotlin/binary-compatibility-validator) to track public API changes.

- **Before submitting a PR**, run:
  ```bash
  ./gradlew -q --console=plain apiCheck
  ```
  This verifies that the public API hasn't changed unexpectedly.
- **If you intentionally changed the public API**, run:
  ```bash
  ./gradlew -q --console=plain apiDump
  ```
  Then commit the updated `.api` files along with your code changes. Your PR description should explain what changed and why.

## Commit Conventions

Use [Conventional Commits](https://www.conventionalcommits.org/) for all commit messages:

| Prefix       | Use for                                  |
|-------------|------------------------------------------|
| `feat:`     | A new feature                            |
| `fix:`      | A bug fix                                |
| `docs:`     | Documentation-only changes               |
| `refactor:` | Code changes that neither fix nor add    |
| `test:`     | Adding or updating tests                 |
| `chore:`    | Build, CI, tooling, or dependency updates|

Examples:
```
feat: add cornerRadius property to HalogenShapes
fix: handle malformed hex colors in SchemaParser
docs: add KDoc to ThemeExpander public methods
refactor: extract tonal palette logic into separate file
test: add contrast validation tests for edge-case colors
chore: update Kotlin to 2.2.20
```

## Pull Request Process

1. **Fork** the repository and create a feature branch from `main`:
   ```bash
   git checkout -b feat/my-feature
   ```
2. **Make your changes**, following the code style and testing guidelines above.
3. **Commit** your changes using conventional commit messages.
4. **Push** your branch to your fork:
   ```bash
   git push origin feat/my-feature
   ```
5. **Open a pull request** against `main` on the upstream repository.

### PR Guidelines

- Include a clear description of what your PR does and why.
- Reference any related GitHub Issues (e.g., "Fixes #42").
- Ensure all CI checks pass (tests, API compatibility, lint).
- Keep PRs focused. One logical change per PR is easier to review than a sprawling multi-concern change.
- Be responsive to review feedback.

## Reporting Bugs

Use [GitHub Issues](https://github.com/himattm/halogen/issues) to report bugs. A good bug report includes:

- A clear, descriptive title.
- The Halogen version, Kotlin version, and platform (Android, iOS, Desktop, Web).
- A **minimal reproduction** -- the smallest possible code that triggers the bug.
- What you expected to happen vs. what actually happened.
- Stack traces or error messages, if applicable.

## Suggesting Features

Feature requests are welcome! Open a [GitHub Issue](https://github.com/himattm/halogen/issues) using the feature request template and describe:

- The problem your feature would solve.
- How you envision it working.
- Any alternatives you've considered.

## License

By contributing to Halogen, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE), the same license that covers the project.
