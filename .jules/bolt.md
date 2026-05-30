## 2024-11-20 - [Optimizing Regex in Kotlin Hot Paths]
**Learning:** In Kotlin multiplatform hot paths, relying heavily on sequential `Regex` operations (e.g. prefix stripping followed by substring processing followed by case conversion) generates immense overhead due to state machine creation, tracking, and backtracking.
**Action:** Always replace heavy regex parsing chains in hot paths with raw manual string traversal via bounded indices and `StringBuilder`. Precalculate maximum string capacities and reduce object allocations to measurably increase execution speed.
