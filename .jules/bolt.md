
## 2024-05-18 - KMP Hex String Allocation Overhead
**Learning:** In Kotlin Multiplatform (specifically evident in WasmJS and Android), standard string operations like `toString(16).padStart(6, '0').uppercase()` or `substring(1).toLong(16).toInt()` in hot paths introduce significant memory allocation overhead. Manual character array mutation and bitwise shifting can achieve massive speedups (up to 30-50x faster) without sacrificing too much readability.
**Action:** When working on parsing or formatting hex codes in KMP hot paths, prefer using pre-sized `CharArray` mutation for formatting and direct string indexing with bitwise accumulation for parsing over Kotlin's standard library string transformation chains.
