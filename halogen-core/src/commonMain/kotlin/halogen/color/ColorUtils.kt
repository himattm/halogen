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

import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.round

internal object ColorUtils {

    val SRGB_TO_XYZ: Array<DoubleArray> = arrayOf(
        doubleArrayOf(0.41233895, 0.35762064, 0.18051042),
        doubleArrayOf(0.2126, 0.7152, 0.0722),
        doubleArrayOf(0.01932141, 0.11916382, 0.95034478),
    )

    val XYZ_TO_SRGB: Array<DoubleArray> = arrayOf(
        doubleArrayOf(3.2413774792388685, -1.5376652402851851, -0.49885366846268053),
        doubleArrayOf(-0.9691452513005321, 1.8758853451067872, 0.04156585616912061),
        doubleArrayOf(0.05562093689691305, -0.20395524564742123, 1.0571799111220335),
    )

    private val WHITE_POINT_D65: DoubleArray = doubleArrayOf(95.047, 100.0, 108.883)

    fun whitePointD65(): DoubleArray = WHITE_POINT_D65

    fun argbFromRgb(red: Int, green: Int, blue: Int): Int =
        (255 shl 24) or ((red and 255) shl 16) or ((green and 255) shl 8) or (blue and 255)

    fun redFromArgb(argb: Int): Int = (argb shr 16) and 255

    fun greenFromArgb(argb: Int): Int = (argb shr 8) and 255

    fun blueFromArgb(argb: Int): Int = argb and 255

    fun isOpaque(argb: Int): Boolean = (argb shr 24 and 255) >= 255

    fun argbFromXyz(x: Double, y: Double, z: Double): Int {
        val matrix = XYZ_TO_SRGB
        val linearR = matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] * z
        val linearG = matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] * z
        val linearB = matrix[2][0] * x + matrix[2][1] * y + matrix[2][2] * z
        val r = delinearized(linearR)
        val g = delinearized(linearG)
        val b = delinearized(linearB)
        return argbFromRgb(r, g, b)
    }

    fun xyzFromArgb(argb: Int): DoubleArray {
        val r = linearized(redFromArgb(argb))
        val g = linearized(greenFromArgb(argb))
        val b = linearized(blueFromArgb(argb))
        return MathUtils.matrixMultiply(doubleArrayOf(r, g, b), SRGB_TO_XYZ)
    }

    fun argbFromLstar(lstar: Double): Int {
        val y = yFromLstar(lstar)
        val component = delinearized(y)
        return argbFromRgb(component, component, component)
    }

    fun lstarFromArgb(argb: Int): Double {
        val y = xyzFromArgb(argb)[1]
        return 116.0 * labF(y / 100.0) - 16.0
    }

    fun yFromLstar(lstar: Double): Double {
        return 100.0 * labInvf((lstar + 16.0) / 116.0)
    }

    fun lstarFromY(y: Double): Double {
        return 116.0 * labF(y / 100.0) - 16.0
    }

    fun linearized(rgbComponent: Int): Double {
        val normalized = rgbComponent / 255.0
        return if (normalized <= 0.040449936) {
            normalized / 12.92 * 100.0
        } else {
            ((normalized + 0.055) / 1.055).pow(2.4) * 100.0
        }
    }

    fun delinearized(rgbComponent: Double): Int {
        val normalized = rgbComponent / 100.0
        val delinearized: Double = if (normalized <= 0.0031308) {
            normalized * 12.92
        } else {
            1.055 * normalized.pow(1.0 / 2.4) - 0.055
        }
        return MathUtils.clampInt(0, 255, round(delinearized * 255.0).toInt())
    }

    private fun labF(t: Double): Double {
        val e = 216.0 / 24389.0
        val kappa = 24389.0 / 27.0
        return if (t > e) {
            cbrt(t)
        } else {
            (kappa * t + 16.0) / 116.0
        }
    }

    private fun labInvf(ft: Double): Double {
        val e = 216.0 / 24389.0
        val kappa = 24389.0 / 27.0
        val ft3 = ft * ft * ft
        return if (ft3 > e) {
            ft3
        } else {
            (116.0 * ft - 16.0) / kappa
        }
    }
}
