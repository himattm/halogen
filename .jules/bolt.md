## 2024-05-19 - Optimize hex color parsing and formatting
**Learning:** Replaced `.substring().toLong(16).toInt()` and `.toString(16).padStart(6, '0').uppercase()` with manual string bitwise iterations reducing overhead and eliminating allocations in repeated `ThemeExpander.kt` hot paths.
**Action:** Avoid expensive string allocation operations inside looping or multi-pass processing functions; use manual arrays or bitshifting instead.
