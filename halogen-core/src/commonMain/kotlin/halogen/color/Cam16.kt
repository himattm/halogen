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
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

internal class Cam16
private constructor(
    val hue: Double,
    val chroma: Double,
    val j: Double,
    val q: Double,
    val m: Double,
    val s: Double,
    val jstar: Double,
    val astar: Double,
    val bstar: Double,
) {
    companion object {
        val XYZ_TO_CAM16RGB: Array<DoubleArray> = arrayOf(
            doubleArrayOf(0.401288, 0.650173, -0.051461),
            doubleArrayOf(-0.250268, 1.204414, 0.045854),
            doubleArrayOf(-0.002079, 0.048952, 0.953127),
        )

        val CAM16RGB_TO_XYZ: Array<DoubleArray> = arrayOf(
            doubleArrayOf(1.8620678, -1.0112547, 0.14918678),
            doubleArrayOf(0.38752654, 0.62144744, -0.00897398),
            doubleArrayOf(-0.01584150, -0.03412294, 1.0499644),
        )

        fun fromInt(argb: Int): Cam16 =
            fromIntInViewingConditions(argb, ViewingConditions.DEFAULT)

        fun fromIntInViewingConditions(argb: Int, viewingConditions: ViewingConditions): Cam16 {
            val red = ColorUtils.redFromArgb(argb)
            val green = ColorUtils.greenFromArgb(argb)
            val blue = ColorUtils.blueFromArgb(argb)
            val redL = ColorUtils.linearized(red)
            val greenL = ColorUtils.linearized(green)
            val blueL = ColorUtils.linearized(blue)
            val x = 0.41233895 * redL + 0.35762064 * greenL + 0.18051042 * blueL
            val y = 0.2126 * redL + 0.7152 * greenL + 0.0722 * blueL
            val z = 0.01932141 * redL + 0.11916382 * greenL + 0.95034478 * blueL
            return fromXyzInViewingConditions(x, y, z, viewingConditions)
        }

        fun fromXyzInViewingConditions(
            x: Double,
            y: Double,
            z: Double,
            viewingConditions: ViewingConditions,
        ): Cam16 {
            val rC = 0.401288 * x + 0.650173 * y + -0.051461 * z
            val gC = -0.250268 * x + 1.204414 * y + 0.045854 * z
            val bC = -0.002079 * x + 0.048952 * y + 0.953127 * z

            val rD = viewingConditions.rgbD[0] * rC
            val gD = viewingConditions.rgbD[1] * gC
            val bD = viewingConditions.rgbD[2] * bC

            val rAF = (viewingConditions.fl * abs(rD) / 100.0).pow(0.42)
            val gAF = (viewingConditions.fl * abs(gD) / 100.0).pow(0.42)
            val bAF = (viewingConditions.fl * abs(bD) / 100.0).pow(0.42)

            val rA = rD.sign * 400.0 * rAF / (rAF + 27.13)
            val gA = gD.sign * 400.0 * gAF / (gAF + 27.13)
            val bA = bD.sign * 400.0 * bAF / (bAF + 27.13)

            val a = (11.0 * rA + -12.0 * gA + bA) / 11.0
            val b = (rA + gA - 2.0 * bA) / 9.0

            val u = (20.0 * rA + 20.0 * gA + 21.0 * bA) / 20.0
            val p2 = (40.0 * rA + 20.0 * gA + bA) / 20.0

            val atan2 = atan2(b, a)
            val atanDegrees = atan2 * 180.0 / PI
            val hue = if (atanDegrees < 0) {
                atanDegrees + 360.0
            } else if (atanDegrees >= 360.0) {
                atanDegrees - 360.0
            } else {
                atanDegrees
            }
            val hueRadians = hue * PI / 180.0

            val ac = p2 * viewingConditions.nbb
            val j = 100.0 * (ac / viewingConditions.aw).pow(viewingConditions.c * viewingConditions.z)

            val q = (4.0 / viewingConditions.c) * sqrt(j / 100.0) *
                (viewingConditions.aw + 4.0) * viewingConditions.flRoot

            val huePrime = if (hue < 20.14) hue + 360.0 else hue
            val eHue = 0.25 * (cos(huePrime * PI / 180.0 + 2.0) + 3.8)
            val p1 = 50000.0 / 13.0 * eHue * viewingConditions.nc * viewingConditions.ncb
            val t = p1 * sqrt(a * a + b * b) / (u + 0.305)
            val alpha = t.pow(0.9) * (1.64 - 0.29.pow(viewingConditions.n)).pow(0.73)

            val c = alpha * sqrt(j / 100.0)
            val m = c * viewingConditions.flRoot
            val s = 50.0 * sqrt(alpha * viewingConditions.c / (viewingConditions.aw + 4.0))

            val jstar = (1.0 + 100.0 * 0.007) * j / (1.0 + 0.007 * j)
            val mstar = 1.0 / 0.0228 * ln(1.0 + 0.0228 * m)
            val astar = mstar * cos(hueRadians)
            val bstar = mstar * sin(hueRadians)

            return Cam16(hue, c, j, q, m, s, jstar, astar, bstar)
        }

        /**
         * Solve for the ARGB color with the given [hue], [chroma], and [lstar] (tone).
         *
         * Uses bisection to find the highest achievable chroma in the sRGB gamut.
         */
        fun toInt(hue: Double, chroma: Double, lstar: Double): Int {
            if (chroma < 1.0 || lstar < 1.0 || lstar > 99.0) {
                return ColorUtils.argbFromLstar(lstar)
            }

            var low = 0.0
            var high = chroma
            var mid: Double
            var bestArgb = ColorUtils.argbFromLstar(lstar)

            for (i in 0 until 16) {
                mid = (low + high) / 2.0
                val candidate = jchToArgbInGamut(hue, mid, lstar)
                if (candidate != null) {
                    bestArgb = candidate
                    low = mid
                } else {
                    high = mid
                }
            }

            val exact = jchToArgbInGamut(hue, chroma, lstar)
            if (exact != null) {
                return exact
            }
            return bestArgb
        }

        private fun jchToArgbInGamut(hue: Double, chroma: Double, lstar: Double): Int? {
            val vc = ViewingConditions.DEFAULT
            val y = ColorUtils.yFromLstar(lstar)
            val j = computeJ(y, vc)

            val alpha = if (j == 0.0) 0.0 else chroma / sqrt(j / 100.0)
            val t = (alpha / (1.64 - 0.29.pow(vc.n)).pow(0.73)).pow(1.0 / 0.9)
            val hRad = hue * PI / 180.0

            val huePrime = if (hue < 20.14) hue + 360.0 else hue
            val eHue = 0.25 * (cos(huePrime * PI / 180.0 + 2.0) + 3.8)
            val ac = vc.aw * (j / 100.0).pow(1.0 / vc.c / vc.z)
            val p1 = eHue * (50000.0 / 13.0) * vc.nc * vc.ncb
            val p2 = ac / vc.nbb

            val hSin = sin(hRad)
            val hCos = cos(hRad)

            val gamma = 23.0 * (p2 + 0.305) * t /
                (23.0 * p1 + 11.0 * t * hCos + 108.0 * t * hSin)
            val a = gamma * hCos
            val b = gamma * hSin

            val rA = (460.0 * p2 + 451.0 * a + 288.0 * b) / 1403.0
            val gA = (460.0 * p2 - 891.0 * a - 261.0 * b) / 1403.0
            val bA = (460.0 * p2 - 220.0 * a - 6300.0 * b) / 1403.0

            val rCBase = max(0.0, (27.13 * abs(rA)) / (400.0 - abs(rA)))
            val rC = rA.sign * (100.0 / vc.fl) * rCBase.pow(1.0 / 0.42)
            val gCBase = max(0.0, (27.13 * abs(gA)) / (400.0 - abs(gA)))
            val gC = gA.sign * (100.0 / vc.fl) * gCBase.pow(1.0 / 0.42)
            val bCBase = max(0.0, (27.13 * abs(bA)) / (400.0 - abs(bA)))
            val bC = bA.sign * (100.0 / vc.fl) * bCBase.pow(1.0 / 0.42)

            val rF = rC / vc.rgbD[0]
            val gF = gC / vc.rgbD[1]
            val bF = bC / vc.rgbD[2]

            val matrix = CAM16RGB_TO_XYZ
            val xR = rF * matrix[0][0] + gF * matrix[0][1] + bF * matrix[0][2]
            val yR = rF * matrix[1][0] + gF * matrix[1][1] + bF * matrix[1][2]
            val zR = rF * matrix[2][0] + gF * matrix[2][1] + bF * matrix[2][2]

            val argb = ColorUtils.argbFromXyz(xR, yR, zR)
            val r = ColorUtils.redFromArgb(argb)
            val g = ColorUtils.greenFromArgb(argb)
            val bVal = ColorUtils.blueFromArgb(argb)

            if (r < 0 || r > 255 || g < 0 || g > 255 || bVal < 0 || bVal > 255) {
                return null
            }
            return argb
        }

        private fun computeJ(y: Double, vc: ViewingConditions): Double {
            val xyz = doubleArrayOf(y, y, y)
            val rT = xyz[0] * XYZ_TO_CAM16RGB[0][0] + xyz[1] * XYZ_TO_CAM16RGB[0][1] + xyz[2] * XYZ_TO_CAM16RGB[0][2]
            val gT = xyz[0] * XYZ_TO_CAM16RGB[1][0] + xyz[1] * XYZ_TO_CAM16RGB[1][1] + xyz[2] * XYZ_TO_CAM16RGB[1][2]
            val bT = xyz[0] * XYZ_TO_CAM16RGB[2][0] + xyz[1] * XYZ_TO_CAM16RGB[2][1] + xyz[2] * XYZ_TO_CAM16RGB[2][2]

            val rD = vc.rgbD[0] * rT
            val gD = vc.rgbD[1] * gT
            val bD = vc.rgbD[2] * bT

            val rAF = (vc.fl * abs(rD) / 100.0).pow(0.42)
            val gAF = (vc.fl * abs(gD) / 100.0).pow(0.42)
            val bAF = (vc.fl * abs(bD) / 100.0).pow(0.42)

            val rA = rD.sign * 400.0 * rAF / (rAF + 27.13)
            val gA = gD.sign * 400.0 * gAF / (gAF + 27.13)
            val bA = bD.sign * 400.0 * bAF / (bAF + 27.13)

            val p2 = (40.0 * rA + 20.0 * gA + bA) / 20.0
            val ac = p2 * vc.nbb
            return 100.0 * (ac / vc.aw).pow(vc.c * vc.z)
        }
    }

    fun viewed(viewingConditions: ViewingConditions): Int {
        val alpha = if (chroma == 0.0 || j == 0.0) {
            0.0
        } else {
            chroma / sqrt(j / 100.0)
        }

        val t = (alpha / (1.64 - 0.29.pow(viewingConditions.n)).pow(0.73)).pow(1.0 / 0.9)
        val hRad = hue * PI / 180.0

        val eHue = 0.25 * (cos(hRad + 2.0) + 3.8)
        val ac = viewingConditions.aw * (j / 100.0).pow(1.0 / viewingConditions.c / viewingConditions.z)
        val p1 = eHue * (50000.0 / 13.0) * viewingConditions.nc * viewingConditions.ncb
        val p2 = ac / viewingConditions.nbb

        val hSin = sin(hRad)
        val hCos = cos(hRad)

        val gamma = 23.0 * (p2 + 0.305) * t / (23.0 * p1 + 11.0 * t * hCos + 108.0 * t * hSin)
        val a = gamma * hCos
        val b = gamma * hSin

        val rA = (460.0 * p2 + 451.0 * a + 288.0 * b) / 1403.0
        val gA = (460.0 * p2 - 891.0 * a - 261.0 * b) / 1403.0
        val bA = (460.0 * p2 - 220.0 * a - 6300.0 * b) / 1403.0

        val rCBase = (27.13 * abs(rA)) / (400.0 - abs(rA))
        val rCScaled = rA.sign * (100.0 / viewingConditions.fl) * rCBase.pow(1.0 / 0.42)

        val gCBase = (27.13 * abs(gA)) / (400.0 - abs(gA))
        val gCScaled = gA.sign * (100.0 / viewingConditions.fl) * gCBase.pow(1.0 / 0.42)

        val bCBase = (27.13 * abs(bA)) / (400.0 - abs(bA))
        val bCScaled = bA.sign * (100.0 / viewingConditions.fl) * bCBase.pow(1.0 / 0.42)

        val linR = rCScaled / viewingConditions.rgbD[0]
        val linG = gCScaled / viewingConditions.rgbD[1]
        val linB = bCScaled / viewingConditions.rgbD[2]

        val x = 1.8620678 * linR + -1.0112547 * linG + 0.14918678 * linB
        val y = 0.38752654 * linR + 0.62144744 * linG + -0.00897398 * linB
        val z = -0.01584150 * linR + -0.03412294 * linG + 1.0499644 * linB

        return ColorUtils.argbFromXyz(x, y, z)
    }
}
