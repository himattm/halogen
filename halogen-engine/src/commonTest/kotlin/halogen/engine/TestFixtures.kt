package halogen.engine

import halogen.HalogenThemeSpec

internal object TestFixtures {
    const val OCEAN_SPEC_JSON =
        """{"pri":"#356A8A","sec":"#5C8A9E","ter":"#7AACB5","neuL":"#F0F5F7","neuD":"#0E1B26","err":"#BA1A1A","font":"modern","hw":500,"bw":400,"ls":false,"cs":"soft","cx":1.2}"""
    const val NEON_SPEC_JSON =
        """{"pri":"#9A6ACD","sec":"#4A8A8A","ter":"#B06B7D","neuL":"#F3F0F6","neuD":"#151018","err":"#93000A","font":"mono","hw":700,"bw":400,"ls":true,"cs":"sharp","cx":0.5}"""

    val OCEAN_SPEC: HalogenThemeSpec by lazy { HalogenThemeSpec.fromJson(OCEAN_SPEC_JSON) }
    val NEON_SPEC: HalogenThemeSpec by lazy { HalogenThemeSpec.fromJson(NEON_SPEC_JSON) }
}
