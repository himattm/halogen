# Custom Prompt Instructions & Token Budget

## Problem

Developers cannot customize the LLM prompt beyond `HalogenConfig.promptGuidance`. They need to add brand constraints ("always use our blue"), domain context ("finance app, prefer blues/greens"), and custom few-shot examples — while Halogen maintains the JSON schema contract.

Additionally, there's no safety check when custom content pushes the prompt past a provider's token limit (Gemini Nano: ~4,000 input tokens).

## Design

### API Surface

Two new methods on `Halogen.Builder`:

```kotlin
val engine = Halogen.Builder()
    .provider(GeminiNanoProvider())
    .promptInstructions("""
        - Always incorporate our brand blue (#1A73E8) as primary
        - This is a finance app — prefer blues, greens, neutral tones
        - Never use playful or mono fonts
    """)
    .promptExamples(
        "quarterly earnings" to """{"pri":"#1A73E8","sec":"#2E5C8A","ter":"#E8710A","neuL":"#F8F9FA","neuD":"#12161C","err":"#BA1A1A","font":"minimal","hw":600,"bw":400,"ls":false,"cs":"sharp","cx":0.8}""",
    )
    .tokenBudget(4000)  // default, Nano's limit
    .build()  // throws if estimated prompt exceeds budget
```

One new public utility on `PromptBuilder`:

```kotlin
PromptBuilder.estimateTokenCount(prompt: String): Int
```

### Prompt Assembly Order

```
1. System prompt (schema + rules)        — LOCKED, not developer-modifiable
2. Config preset guidance                 — from HalogenConfig.promptGuidance
3. Developer instructions                 — from .promptInstructions()
4. Built-in few-shot examples (3)         — LOCKED, always present
5. Developer few-shot examples            — from .promptExamples()
6. Extension definitions                  — from .extensions()
7. Style prefix + user hint               — from config.styleName + resolve(hint)
```

### Token Budget Validation

At `build()` time:
1. Assemble a sample prompt using a placeholder hint ("test")
2. Estimate token count (~4 chars per token heuristic)
3. If over budget, throw `IllegalStateException` with a breakdown

```
IllegalStateException: Estimated prompt exceeds token budget (1,450 / 4,000 tokens)
  System prompt:     ~400 tokens
  Config guidance:    ~30 tokens
  Instructions:      ~200 tokens
  Built-in examples: ~450 tokens
  Custom examples:   ~300 tokens
  Extensions (5):    ~75 tokens
Reduce promptInstructions or promptExamples, or increase tokenBudget.
```

Cloud providers can disable: `.tokenBudget(Int.MAX_VALUE)`.

### Files Modified

| File | Change |
|------|--------|
| `halogen-core/…/PromptBuilder.kt` | Add `additionalInstructions` and `customExamples` params to `build()`. Add public `estimateTokenCount()`. |
| `halogen-engine/…/Halogen.kt` | Add `promptInstructions()`, `promptExamples()`, `tokenBudget()` to Builder. Validate at `build()`. |
| `halogen-engine/…/HalogenEngine.kt` | Store and pass new fields to `PromptBuilder.build()`. |

### What Stays Locked

- System prompt (schema, color rules, format requirements)
- Built-in 3 few-shot examples (always present as baseline)
- SchemaParser validation (hex format, value clamping)
- JSON field names (pri, sec, ter, etc.)
