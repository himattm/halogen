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
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal object HctSolver {

    private val SCALED_DISCOUNT_FROM_LINRGB: Array<DoubleArray> = arrayOf(
        doubleArrayOf(0.001200833568784504, 0.002389694492170889, 0.0002795742885861124),
        doubleArrayOf(0.0005891086651375999, 0.0029785502573438758, 0.0003270666104008398),
        doubleArrayOf(0.00010146692491640572, 0.0005364214359186694, 0.0032979401770712076),
    )

    private val LINRGB_FROM_SCALED_DISCOUNT: Array<DoubleArray> = arrayOf(
        doubleArrayOf(1373.2198709594231, -1100.4251190754821, -7.278681089101213),
        doubleArrayOf(-271.815969077903, 559.6580465940733, -32.46047482621506),
        doubleArrayOf(1.9622899599665666, -57.173814538844006, 308.7233197812385),
    )

    private val Y_FROM_LINRGB: DoubleArray = doubleArrayOf(0.2126, 0.7152, 0.0722)

    /**
     * Critical planes: linearized sRGB values for each integer sRGB value 0..255.
     * Index i holds the linearized value of sRGB component value i.
     */
    private val CRITICAL_PLANES: DoubleArray = DoubleArray(256) { i ->
        ColorUtils.linearized(i)
    }

    fun solveToInt(hueDegrees: Double, chroma: Double, lstar: Double): Int {
        if (chroma < 0.0001 || lstar < 0.0001 || lstar > 99.9999) {
            return ColorUtils.argbFromLstar(lstar)
        }
        val hueRadians = hueDegrees / 180.0 * PI
        val y = ColorUtils.yFromLstar(lstar)
        val exactAnswer = findResultByJ(hueRadians, chroma, y)
        if (exactAnswer != 0) return exactAnswer

        val linrgb = bisectToLimit(y, hueRadians)
        return ColorUtils.argbFromRgb(
            ColorUtils.delinearized(linrgb[0]),
            ColorUtils.delinearized(linrgb[1]),
            ColorUtils.delinearized(linrgb[2]),
        )
    }

    fun solveToCam(hueDegrees: Double, chroma: Double, lstar: Double): Cam16 {
        return Cam16.fromInt(solveToInt(hueDegrees, chroma, lstar))
    }

    private fun chromaticAdaptation(component: Double): Double {
        val af = abs(component).pow(0.42)
        return signum(component) * 400.0 * af / (af + 27.13)
    }

    private fun inverseChromaticAdaptation(adapted: Double): Double {
        val adaptedAbs = abs(adapted)
        val base = 27.13 * adaptedAbs / (400.0 - adaptedAbs)
        return signum(adapted) * base.pow(1.0 / 0.42)
    }

    private fun hueOf(linrgb: DoubleArray): Double {
        val scaledDiscount = MathUtils.matrixMultiply(linrgb, SCALED_DISCOUNT_FROM_LINRGB)
        val rA = chromaticAdaptation(scaledDiscount[0])
        val gA = chromaticAdaptation(scaledDiscount[1])
        val bA = chromaticAdaptation(scaledDiscount[2])

        val a = (11.0 * rA + -12.0 * gA + bA) / 11.0
        val b = (rA + gA - 2.0 * bA) / 9.0
        return atan2(b, a)
    }

    private fun areInCyclicOrder(a: Double, b: Double, c: Double): Boolean {
        val deltaAB = sanitizeRadians(b - a)
        val deltaAC = sanitizeRadians(c - a)
        return deltaAB < deltaAC
    }

    private fun sanitizeRadians(angle: Double): Double {
        return (angle + PI * 8.0) % (2.0 * PI)
    }

    private fun trueDelinearized(rgbComponent: Double): Double {
        val normalized = rgbComponent / 100.0
        val delinearized = if (normalized <= 0.0031308) {
            normalized * 12.92
        } else {
            1.055 * normalized.pow(1.0 / 2.4) - 0.055
        }
        return delinearized * 255.0
    }

    private fun nthVertex(y: Double, n: Int): DoubleArray {
        val kR = Y_FROM_LINRGB[0]
        val kG = Y_FROM_LINRGB[1]
        val kB = Y_FROM_LINRGB[2]

        val coordA = if (n % 4 <= 1) 0.0 else 100.0
        val coordB = if (n % 2 == 0) 0.0 else 100.0

        return when {
            n < 4 -> {
                val g = coordA
                val b = coordB
                val r = (y - g * kG - b * kB) / kR
                if (r in 0.0..100.0) doubleArrayOf(r, g, b) else doubleArrayOf(-1.0, -1.0, -1.0)
            }
            n < 8 -> {
                val b = coordA
                val r = coordB
                val g = (y - r * kR - b * kB) / kG
                if (g in 0.0..100.0) doubleArrayOf(r, g, b) else doubleArrayOf(-1.0, -1.0, -1.0)
            }
            else -> {
                val r = coordA
                val g = coordB
                val b = (y - r * kR - g * kG) / kB
                if (b in 0.0..100.0) doubleArrayOf(r, g, b) else doubleArrayOf(-1.0, -1.0, -1.0)
            }
        }
    }

    private fun bisectToSegment(y: Double, targetHue: Double): DoubleArray {
        var left = doubleArrayOf(-1.0, -1.0, -1.0)
        var right = left
        var leftHue = 0.0
        var rightHue = 0.0
        var initialized = false
        var uncut = true

        for (n in 0 until 12) {
            val mid = nthVertex(y, n)
            if (mid[0] < 0) continue

            val midHue = hueOf(mid)
            if (!initialized) {
                left = mid
                right = mid
                leftHue = midHue
                rightHue = midHue
                initialized = true
                continue
            }

            if (uncut || areInCyclicOrder(leftHue, midHue, rightHue)) {
                uncut = false
                if (areInCyclicOrder(leftHue, targetHue, midHue)) {
                    right = mid
                    rightHue = midHue
                } else {
                    left = mid
                    leftHue = midHue
                }
            }
        }

        return doubleArrayOf(
            left[0], left[1], left[2],
            right[0], right[1], right[2],
        )
    }

    private fun midpoint(a: DoubleArray, b: DoubleArray): DoubleArray {
        return doubleArrayOf(
            (a[0] + b[0]) / 2.0,
            (a[1] + b[1]) / 2.0,
            (a[2] + b[2]) / 2.0,
        )
    }

    private fun criticalPlaneBelow(x: Double): Int {
        return floor(x - 0.5).toInt()
    }

    private fun criticalPlaneAbove(x: Double): Int {
        return ceil(x - 0.5).toInt()
    }

    private fun bisectToLimit(y: Double, targetHue: Double): DoubleArray {
        val segment = bisectToSegment(y, targetHue)
        var left = doubleArrayOf(segment[0], segment[1], segment[2])
        var leftHue = hueOf(left)
        var right = doubleArrayOf(segment[3], segment[4], segment[5])

        for (axis in 0..2) {
            if (left[axis] != right[axis]) {
                var lPlane: Int
                var rPlane: Int
                if (left[axis] < right[axis]) {
                    lPlane = criticalPlaneBelow(trueDelinearized(left[axis]))
                    rPlane = criticalPlaneAbove(trueDelinearized(right[axis]))
                } else {
                    lPlane = criticalPlaneAbove(trueDelinearized(left[axis]))
                    rPlane = criticalPlaneBelow(trueDelinearized(right[axis]))
                }
                for (i in 0 until 8) {
                    if (abs(rPlane - lPlane) <= 1) break
                    val mPlane = floor((lPlane + rPlane) / 2.0).toInt()
                    val midPlaneCoordinate = CRITICAL_PLANES[mPlane]
                    val mid = left.copyOf()
                    mid[axis] = midPlaneCoordinate
                    val midHue = hueOf(mid)
                    if (areInCyclicOrder(leftHue, targetHue, midHue)) {
                        right = mid
                        rPlane = mPlane
                    } else {
                        left = mid
                        leftHue = midHue
                        lPlane = mPlane
                    }
                }
            }
        }
        return midpoint(left, right)
    }

    private fun findResultByJ(hueRadians: Double, chroma: Double, y: Double): Int {
        var j = sqrt(y) * 11.0
        val vc = ViewingConditions.DEFAULT
        val tInnerCoeff = 1.0 / (1.64 - 0.29.pow(vc.n)).pow(0.73)

        val eHue = 0.25 * (cos(hueRadians + 2.0) + 3.8)
        val p1 = eHue * (50000.0 / 13.0) * vc.nc * vc.ncb
        val hSin = sin(hueRadians)
        val hCos = cos(hueRadians)

        for (iterationRound in 0 until 5) {
            val jNormalized = j / 100.0
            val alpha = if (chroma == 0.0 || j == 0.0) 0.0 else chroma / sqrt(jNormalized)

            val t = (alpha * tInnerCoeff).pow(1.0 / 0.9)
            val ac = vc.aw * jNormalized.pow(1.0 / vc.c / vc.z)
            val p2 = ac / vc.nbb

            val gamma = 23.0 * (p2 + 0.305) * t / (23.0 * p1 + 11.0 * t * hCos + 108.0 * t * hSin)
            val a = gamma * hCos
            val b = gamma * hSin

            val rA = (460.0 * p2 + 451.0 * a + 288.0 * b) / 1403.0
            val gA = (460.0 * p2 - 891.0 * a - 261.0 * b) / 1403.0
            val bA = (460.0 * p2 - 220.0 * a - 6300.0 * b) / 1403.0

            val rCScaled = inverseChromaticAdaptation(rA)
            val gCScaled = inverseChromaticAdaptation(gA)
            val bCScaled = inverseChromaticAdaptation(bA)

            val linR = rCScaled * LINRGB_FROM_SCALED_DISCOUNT[0][0] +
                gCScaled * LINRGB_FROM_SCALED_DISCOUNT[0][1] +
                bCScaled * LINRGB_FROM_SCALED_DISCOUNT[0][2]
            val linG = rCScaled * LINRGB_FROM_SCALED_DISCOUNT[1][0] +
                gCScaled * LINRGB_FROM_SCALED_DISCOUNT[1][1] +
                bCScaled * LINRGB_FROM_SCALED_DISCOUNT[1][2]
            val linB = rCScaled * LINRGB_FROM_SCALED_DISCOUNT[2][0] +
                gCScaled * LINRGB_FROM_SCALED_DISCOUNT[2][1] +
                bCScaled * LINRGB_FROM_SCALED_DISCOUNT[2][2]

            if (linR < 0 || linG < 0 || linB < 0) {
                return 0
            }

            val kR = Y_FROM_LINRGB[0]
            val kG = Y_FROM_LINRGB[1]
            val kB = Y_FROM_LINRGB[2]
            val fnj = kR * linR + kG * linG + kB * linB
            if (fnj <= 0) return 0

            if (iterationRound == 4 || abs(fnj - y) < 0.002) {
                if (linR > 100.01 || linG > 100.01 || linB > 100.01) {
                    return 0
                }
                return ColorUtils.argbFromRgb(
                    ColorUtils.delinearized(linR),
                    ColorUtils.delinearized(linG),
                    ColorUtils.delinearized(linB),
                )
            }
            // Newton's method step
            j = j - (fnj - y) * j / (2.0 * fnj)
        }
        return 0
    }

    private fun signum(num: Double): Double = when {
        num < 0.0 -> -1.0
        num == 0.0 -> 0.0
        else -> 1.0
    }
}
