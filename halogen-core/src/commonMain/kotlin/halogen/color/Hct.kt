/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package halogen.color

/**
 * A color system built using CAM16 hue and chroma, and L* (CIELAB lightness) as the tone.
 *
 * Using L* creates a link between the color system, contrast, and thus readability. Empirically,
 * colors with an idealized L* difference of 40 have a contrast ratio of at least 3.0 and look
 * distinct.
 */
public class Hct private constructor(argb: Int) {
    /** The hue of this color in degrees, from 0 to 360. */
    public var hue: Double = 0.0
        private set

    /** The chroma of this color. Chroma is unbounded; the maximum depends on hue and tone. */
    public var chroma: Double = 0.0
        private set

    /** The tone (lightness) of this color, from 0 to 100. */
    public var tone: Double = 0.0
        private set

    private var argb: Int = argb

    init {
        setInternalState(argb)
    }

    /**
     * Returns the ARGB integer representation of this color.
     */
    public fun toInt(): Int = argb

    /**
     * Set the hue of this color. Modifies the underlying ARGB representation.
     */
    public fun setHue(newHue: Double) {
        setInternalState(HctSolver.solveToInt(newHue, chroma, tone))
    }

    /**
     * Set the chroma of this color. Modifies the underlying ARGB representation.
     */
    public fun setChroma(newChroma: Double) {
        setInternalState(HctSolver.solveToInt(hue, newChroma, tone))
    }

    /**
     * Set the tone of this color. Modifies the underlying ARGB representation.
     */
    public fun setTone(newTone: Double) {
        setInternalState(HctSolver.solveToInt(hue, chroma, newTone))
    }

    private fun setInternalState(argb: Int) {
        this.argb = argb
        val cam = Cam16.fromInt(argb)
        hue = cam.hue
        chroma = cam.chroma
        tone = ColorUtils.lstarFromArgb(argb)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Hct) return false
        return argb == other.argb
    }

    override fun hashCode(): Int = argb

    override fun toString(): String = "Hct(h=$hue, c=$chroma, t=$tone)"

    public companion object {
        /**
         * Create an HCT color from hue, chroma, and tone.
         *
         * @param hue 0 <= hue < 360; invalid values are corrected.
         * @param chroma 0 <= chroma < ?; Informally, colorfulness. The color returned may have
         *   a lower chroma than requested if it is not achievable in sRGB.
         * @param tone 0 <= tone <= 100; informally, lightness. Invalid values are corrected.
         * @return HCT representation of a color in default viewing conditions.
         */
        /**
         * Maximum acceptable hue deviation before we consider the solver result wrong.
         * Yellow/green hues (60-120) are particularly prone to solver errors at low tones.
         */
        private const val MAX_HUE_DEVIATION = 20.0

        public fun from(hue: Double, chroma: Double, tone: Double): Hct {
            val argb = HctSolver.solveToInt(hue, chroma, tone)
            val result = Hct(argb)

            // Verify the solver produced the right hue. The HCT solver can converge
            // to wrong hues at certain tone/chroma combinations, especially:
            // - Yellow/amber hues at low tones
            // - High-chroma colors at extreme tones (gamut clipping)
            if (chroma > 1.0 && result.chroma > 1.0) {
                val hueDiff = hueDifference(hue, result.hue)
                if (hueDiff > MAX_HUE_DEVIATION) {
                    val corrected = correctHue(hue, chroma, tone)
                    if (corrected != null) return corrected
                }
            }

            return result
        }

        /**
         * When the solver produces a wrong hue, try two strategies:
         * 1. Search nearby tones at the same chroma for a correct-hue result.
         * 2. Reduce chroma iteratively — high chroma at extreme tones often
         *    exceeds sRGB gamut, causing hue shifts. Find the max achievable
         *    chroma that keeps the correct hue.
         */
        private fun correctHue(targetHue: Double, targetChroma: Double, targetTone: Double): Hct? {
            // Strategy 1: nearby tones at full chroma
            var bestResult: Hct? = null
            var bestScore = Double.MAX_VALUE

            for (offset in 0..20) {
                for (tryTone in listOfNotNull(
                    if (targetTone + offset <= 100) targetTone + offset else null,
                    if (offset > 0 && targetTone - offset >= 0) targetTone - offset else null,
                )) {
                    val tryArgb = HctSolver.solveToInt(targetHue, targetChroma, tryTone)
                    val tryHct = Hct(tryArgb)
                    val hueDiff = hueDifference(targetHue, tryHct.hue)
                    if (hueDiff <= MAX_HUE_DEVIATION) {
                        val toneDiff = kotlin.math.abs(tryHct.tone - targetTone)
                        if (toneDiff < bestScore) {
                            bestScore = toneDiff
                            bestResult = tryHct
                        }
                    }
                }
                if (bestResult != null && bestScore < 5.0) break
            }
            if (bestResult != null) return bestResult

            // Strategy 2: reduce chroma until hue is correct
            var tryChroma = targetChroma
            while (tryChroma > 1.0) {
                tryChroma *= 0.8
                val tryArgb = HctSolver.solveToInt(targetHue, tryChroma, targetTone)
                val tryHct = Hct(tryArgb)
                val hueDiff = hueDifference(targetHue, tryHct.hue)
                if (hueDiff <= MAX_HUE_DEVIATION) {
                    return tryHct
                }
            }

            return null
        }

        /** Shortest angular distance between two hues. */
        private fun hueDifference(a: Double, b: Double): Double {
            return 180.0 - kotlin.math.abs(kotlin.math.abs(a - b) - 180.0)
        }

        /**
         * Create an HCT color from an ARGB integer.
         *
         * @param argb ARGB representation of a color.
         * @return HCT representation of a color in default viewing conditions.
         */
        public fun fromInt(argb: Int): Hct {
            return Hct(argb)
        }
    }
}
