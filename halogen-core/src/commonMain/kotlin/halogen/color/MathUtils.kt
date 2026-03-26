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

import kotlin.math.abs
import kotlin.math.floor

internal object MathUtils {

    fun signum(num: Double): Int = when {
        num < 0.0 -> -1
        num == 0.0 -> 0
        else -> 1
    }

    fun lerp(start: Double, stop: Double, amount: Double): Double =
        (1.0 - amount) * start + amount * stop

    fun clampInt(min: Int, max: Int, input: Int): Int = when {
        input < min -> min
        input > max -> max
        else -> input
    }

    fun clampDouble(min: Double, max: Double, input: Double): Double = when {
        input < min -> min
        input > max -> max
        else -> input
    }

    fun sanitizeDegreesInt(degrees: Int): Int {
        var d = degrees % 360
        if (d < 0) d += 360
        return d
    }

    fun sanitizeDegreesDouble(degrees: Double): Double {
        var d = degrees % 360.0
        if (d < 0.0) d += 360.0
        return d
    }

    fun rotationDirection(from: Double, to: Double): Double {
        val increasingDifference = sanitizeDegreesDouble(to - from)
        return if (increasingDifference <= 180.0) 1.0 else -1.0
    }

    fun differenceDegrees(a: Double, b: Double): Double =
        180.0 - abs(abs(a - b) - 180.0)

    fun matrixMultiply(row: DoubleArray, matrix: Array<DoubleArray>): DoubleArray {
        val a = row[0] * matrix[0][0] + row[1] * matrix[0][1] + row[2] * matrix[0][2]
        val b = row[0] * matrix[1][0] + row[1] * matrix[1][1] + row[2] * matrix[1][2]
        val c = row[0] * matrix[2][0] + row[1] * matrix[2][1] + row[2] * matrix[2][2]
        return doubleArrayOf(a, b, c)
    }
}
