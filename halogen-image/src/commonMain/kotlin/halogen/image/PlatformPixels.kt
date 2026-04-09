package halogen.image

import coil3.ImageLoader
import coil3.PlatformContext

/**
 * Load an image URL via Coil and extract its ARGB pixel data.
 * Platform-specific: uses Bitmap on Android, Skia Bitmap on JVM/iOS/wasmJs.
 *
 * @return a [PixelData] containing the ARGB pixels, width, and height,
 *         or `null` if loading fails.
 */
internal expect suspend fun loadPixels(
    url: String,
    imageLoader: ImageLoader,
    context: PlatformContext,
): PixelData?

/**
 * Raw pixel data extracted from an image.
 *
 * @property pixels ARGB-packed pixel values, length = [width] * [height].
 * @property width  The image width in pixels.
 * @property height The image height in pixels.
 */
internal class PixelData(
    val pixels: IntArray,
    val width: Int,
    val height: Int,
)
