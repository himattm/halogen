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

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

internal class ViewingConditions
private constructor(
    val n: Double,
    val aw: Double,
    val nbb: Double,
    val ncb: Double,
    val c: Double,
    val nc: Double,
    val rgbD: DoubleArray,
    val fl: Double,
    val flRoot: Double,
    val z: Double,
) {
    companion object {
        val DEFAULT: ViewingConditions = make(
            whitePoint = ColorUtils.whitePointD65(),
            adaptingLuminance = (200.0 / PI) * ColorUtils.yFromLstar(50.0) / 100.0,
            backgroundLstar = 50.0,
            surround = 2.0,
            discountingIlluminant = false,
        )

        fun make(
            whitePoint: DoubleArray,
            adaptingLuminance: Double,
            backgroundLstar: Double,
            surround: Double,
            discountingIlluminant: Boolean,
        ): ViewingConditions {
            val matrix = Cam16.XYZ_TO_CAM16RGB
            val rW = whitePoint[0] * matrix[0][0] + whitePoint[1] * matrix[0][1] + whitePoint[2] * matrix[0][2]
            val gW = whitePoint[0] * matrix[1][0] + whitePoint[1] * matrix[1][1] + whitePoint[2] * matrix[1][2]
            val bW = whitePoint[0] * matrix[2][0] + whitePoint[1] * matrix[2][1] + whitePoint[2] * matrix[2][2]

            val f = 0.8 + surround / 10.0
            val c = if (f >= 0.9) {
                MathUtils.lerp(0.59, 0.69, (f - 0.9) * 10.0)
            } else {
                MathUtils.lerp(0.525, 0.59, (f - 0.8) * 10.0)
            }
            var d = if (discountingIlluminant) {
                1.0
            } else {
                f * (1.0 - (1.0 / 3.6) * exp((-adaptingLuminance - 42.0) / 92.0))
            }
            d = MathUtils.clampDouble(0.0, 1.0, d)

            val nc = f
            val rgbD = doubleArrayOf(
                d * (100.0 / rW) + 1.0 - d,
                d * (100.0 / gW) + 1.0 - d,
                d * (100.0 / bW) + 1.0 - d,
            )

            val k = 1.0 / (5.0 * adaptingLuminance + 1.0)
            val k4 = k * k * k * k
            val k4F = 1.0 - k4
            val fl = k4 * adaptingLuminance + 0.1 * k4F * k4F * cbrt(5.0 * adaptingLuminance)
            val n = ColorUtils.yFromLstar(backgroundLstar) / whitePoint[1]
            val z = 1.48 + sqrt(n)
            val nbb = 0.725 / n.pow(0.2)
            val ncb = nbb

            val rgbAFactors = doubleArrayOf(
                (fl * rgbD[0] * rW / 100.0).pow(0.42),
                (fl * rgbD[1] * gW / 100.0).pow(0.42),
                (fl * rgbD[2] * bW / 100.0).pow(0.42),
            )

            val rgbA = doubleArrayOf(
                400.0 * rgbAFactors[0] / (rgbAFactors[0] + 27.13),
                400.0 * rgbAFactors[1] / (rgbAFactors[1] + 27.13),
                400.0 * rgbAFactors[2] / (rgbAFactors[2] + 27.13),
            )

            val aw = (2.0 * rgbA[0] + rgbA[1] + 0.05 * rgbA[2]) * nbb

            return ViewingConditions(
                n = n,
                aw = aw,
                nbb = nbb,
                ncb = ncb,
                c = c,
                nc = nc,
                rgbD = rgbD,
                fl = fl,
                flRoot = fl.pow(0.25),
                z = z,
            )
        }
    }
}
