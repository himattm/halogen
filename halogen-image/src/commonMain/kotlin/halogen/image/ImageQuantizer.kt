package halogen.image

import halogen.color.Hct
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Extracts dominant colors from raw image pixel data using histogram bucketing
 * and k-means clustering in HCT color space.
 *
 * The algorithm:
 * 1. Subsamples large images to keep processing fast.
 * 2. Builds a 15-bit histogram (5 bits per RGB channel) of opaque pixels.
 * 3. Takes the top 64 histogram entries by population.
 * 4. Converts each to HCT color space for perceptually uniform clustering.
 * 5. Runs k-means++ in weighted HCT space (hue-wrapping aware).
 * 6. Filters low-chroma clusters and returns results sorted by population.
 */
public object ImageQuantizer {

    /** Maximum number of pixels to process before subsampling kicks in. */
    private const val MAX_PIXELS = 16_384

    /** Maximum histogram entries to feed into k-means. */
    private const val MAX_HISTOGRAM_ENTRIES = 64

    /** Maximum k-means iterations. */
    private const val MAX_ITERATIONS = 10

    /** Weights for the HCT distance function. */
    private const val HUE_WEIGHT = 1.0
    private const val CHROMA_WEIGHT = 1.0
    private const val TONE_WEIGHT = 0.5

    /**
     * An HCT entry from the histogram, carrying its pixel count for weighted clustering.
     */
    private class HctEntry(
        val hue: Double,
        val chroma: Double,
        val tone: Double,
        val argb: Int,
        val count: Int,
    )

    /**
     * Extract dominant colors from an image's raw ARGB pixel array.
     *
     * @param pixels ARGB pixel data in row-major order.
     * @param width Image width in pixels.
     * @param height Image height in pixels.
     * @param maxColors Maximum number of dominant colors to extract. Defaults to 6.
     * @param minChroma Minimum HCT chroma for a cluster centroid to be kept.
     *   Colors below this threshold are considered too gray. Defaults to 8.0.
     * @return A [DominantColors] containing the extracted palette sorted by population.
     */
    public fun extract(
        pixels: IntArray,
        width: Int,
        height: Int,
        maxColors: Int = 6,
        minChroma: Double = 8.0,
    ): DominantColors {
        require(width > 0 && height > 0) { "Image dimensions must be positive" }
        require(pixels.size >= width * height) { "Pixel array too small for given dimensions" }

        // Step 1: Subsample if needed
        val totalPixels = width.toLong() * height
        val stride = if (totalPixels > MAX_PIXELS) {
            ((totalPixels + MAX_PIXELS - 1) / MAX_PIXELS).toInt().coerceAtLeast(1)
        } else {
            1
        }

        // Step 2: Build 15-bit histogram, skipping transparent pixels
        val histogram = HashMap<Int, Int>(256)
        var sampledCount = 0
        val pixelCount = width * height
        var i = 0
        while (i < pixelCount) {
            val argb = pixels[i]
            val alpha = (argb ushr 24) and 0xFF
            if (alpha >= 128) {
                val key = bucketKey(argb)
                histogram[key] = (histogram[key] ?: 0) + 1
                sampledCount++
            }
            i += stride
        }

        if (sampledCount == 0) {
            return DominantColors(emptyList())
        }

        // Step 3: Take top 64 populated entries
        val topEntries = histogram.entries
            .sortedByDescending { it.value }
            .take(MAX_HISTOGRAM_ENTRIES)

        // Step 4: Convert bucket keys to representative ARGB, then to HCT
        val entries = topEntries.map { (key, count) ->
            val argb = bucketToArgb(key)
            val hct = Hct.fromInt(argb)
            HctEntry(hct.hue, hct.chroma, hct.tone, argb, count)
        }

        val k = maxColors.coerceAtMost(entries.size)
        if (k == 0) return DominantColors(emptyList())

        // Step 5: K-means++ seeding
        val centroids = initKMeansPlusPlus(entries, k)

        // Step 6: K-means clustering
        repeat(MAX_ITERATIONS) {
            // Assign each entry to nearest centroid
            val assignments = IntArray(entries.size) { idx ->
                nearestCentroid(entries[idx], centroids)
            }

            // Recompute centroids using weighted circular mean for hue
            var converged = true
            for (c in centroids.indices) {
                var sumHueSin = 0.0
                var sumHueCos = 0.0
                var sumChroma = 0.0
                var sumTone = 0.0
                var totalWeight = 0.0

                for (idx in entries.indices) {
                    if (assignments[idx] == c) {
                        val w = entries[idx].count.toDouble()
                        val hueRad = entries[idx].hue * PI / 180.0
                        sumHueSin += sin(hueRad) * w
                        sumHueCos += cos(hueRad) * w
                        sumChroma += entries[idx].chroma * w
                        sumTone += entries[idx].tone * w
                        totalWeight += w
                    }
                }

                if (totalWeight > 0) {
                    val newHue = (atan2(sumHueSin, sumHueCos) * 180.0 / PI + 360.0) % 360.0
                    val newChroma = sumChroma / totalWeight
                    val newTone = sumTone / totalWeight

                    if (hueDist(centroids[c][0], newHue) > 0.5 ||
                        abs(centroids[c][1] - newChroma) > 0.5 ||
                        abs(centroids[c][2] - newTone) > 0.5
                    ) {
                        converged = false
                    }
                    centroids[c][0] = newHue
                    centroids[c][1] = newChroma
                    centroids[c][2] = newTone
                }
            }

            if (converged) return@repeat
        }

        // Compute final assignments and population per cluster
        val clusterPopulations = DoubleArray(k)
        for (idx in entries.indices) {
            val c = nearestCentroid(entries[idx], centroids)
            clusterPopulations[c] += entries[idx].count.toDouble()
        }

        // Convert centroids to ARGB via HCT
        val clusterArgb = IntArray(k) { c ->
            Hct.from(centroids[c][0], centroids[c][1], centroids[c][2]).toInt()
        }

        val totalPop = clusterPopulations.sum()

        // Step 7 & 8: Filter by chroma and build result sorted by population descending
        val filtered = centroids.indices
            .filter { centroids[it][1] >= minChroma }
            .map { c ->
                QuantizedColor(
                    argb = clusterArgb[c],
                    population = if (totalPop > 0) clusterPopulations[c] / totalPop else 0.0,
                    hue = centroids[c][0],
                    chroma = centroids[c][1],
                    tone = centroids[c][2],
                )
            }
            .sortedByDescending { it.population }

        // Re-normalize populations to sum to 1.0 after chroma filtering
        val filteredSum = filtered.sumOf { it.population }
        val colors = if (filteredSum > 0.0 && filteredSum != 1.0) {
            filtered.map { it.copy(population = it.population / filteredSum) }
        } else {
            filtered
        }

        return DominantColors(colors)
    }

    // ---- Histogram bucketing ----

    /**
     * Bucket an ARGB color to a 15-bit key (5 bits per channel, dropping the lowest 3 bits).
     */
    private fun bucketKey(argb: Int): Int {
        val r = (argb ushr 19) and 0x1F
        val g = (argb ushr 11) and 0x1F
        val b = (argb ushr 3) and 0x1F
        return (r shl 10) or (g shl 5) or b
    }

    /**
     * Convert a 15-bit bucket key back to a representative ARGB value.
     * Restores each 5-bit channel to 8 bits by shifting left 3 and filling the low bits.
     */
    private fun bucketToArgb(key: Int): Int {
        val r5 = (key ushr 10) and 0x1F
        val g5 = (key ushr 5) and 0x1F
        val b5 = key and 0x1F
        val r = (r5 shl 3) or (r5 ushr 2)
        val g = (g5 shl 3) or (g5 ushr 2)
        val b = (b5 shl 3) or (b5 ushr 2)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    // ---- HCT distance ----

    /**
     * Weighted squared distance in HCT space with hue wrapping.
     */
    private fun hctDistanceSq(
        h1: Double, c1: Double, t1: Double,
        h2: Double, c2: Double, t2: Double,
    ): Double {
        val dh = hueDist(h1, h2)
        val dc = c1 - c2
        val dt = t1 - t2
        return HUE_WEIGHT * dh * dh + CHROMA_WEIGHT * dc * dc + TONE_WEIGHT * dt * dt
    }

    /**
     * Find the index of the nearest centroid for a given entry.
     */
    private fun nearestCentroid(entry: HctEntry, centroids: Array<DoubleArray>): Int {
        var bestDist = Double.MAX_VALUE
        var bestIdx = 0
        for (c in centroids.indices) {
            val d = hctDistanceSq(
                entry.hue, entry.chroma, entry.tone,
                centroids[c][0], centroids[c][1], centroids[c][2],
            )
            if (d < bestDist) {
                bestDist = d
                bestIdx = c
            }
        }
        return bestIdx
    }

    // ---- K-means++ initialization ----

    /**
     * K-means++ seeding: pick the first centroid as the highest-population entry,
     * then pick subsequent centroids with probability proportional to the squared
     * distance from the nearest existing centroid, weighted by entry count.
     */
    private fun initKMeansPlusPlus(entries: List<HctEntry>, k: Int): Array<DoubleArray> {
        val centroids = Array(k) { DoubleArray(3) }

        // First centroid: highest population entry
        val first = entries.maxBy { it.count }
        centroids[0] = doubleArrayOf(first.hue, first.chroma, first.tone)

        val distances = DoubleArray(entries.size) { Double.MAX_VALUE }

        for (c in 1 until k) {
            // Update distances: for each entry, find min distance to existing centroids
            for (idx in entries.indices) {
                val d = hctDistanceSq(
                    entries[idx].hue, entries[idx].chroma, entries[idx].tone,
                    centroids[c - 1][0], centroids[c - 1][1], centroids[c - 1][2],
                )
                if (d < distances[idx]) {
                    distances[idx] = d
                }
            }

            // Pick next centroid: deterministic -- choose entry with max weighted distance
            // (Deterministic variant of k-means++ for reproducible results)
            var bestIdx = 0
            var bestWeightedDist = -1.0
            for (idx in entries.indices) {
                val wd = distances[idx] * entries[idx].count
                if (wd > bestWeightedDist) {
                    bestWeightedDist = wd
                    bestIdx = idx
                }
            }

            centroids[c] = doubleArrayOf(
                entries[bestIdx].hue,
                entries[bestIdx].chroma,
                entries[bestIdx].tone,
            )
        }

        return centroids
    }
}
