## 2026-07-27 - [ColorUtils linearization optimization]
**Learning:** In Kotlin Multiplatform hot paths (such as `ColorUtils.linearized`), mathematical operations (divisions, `.pow()`) that depend strictly on a discrete domain (0-255) can be effectively replaced by a lookup table (LUT) such as a pre-computed `DoubleArray(256)`.
**Action:** When performing similar mathematical operations that take a discrete 8-bit parameter, consider employing LUTs initialized via array factory functions. Avoid applying LUTs to continuous domains (e.g. `Double`) to prevent precision loss.
