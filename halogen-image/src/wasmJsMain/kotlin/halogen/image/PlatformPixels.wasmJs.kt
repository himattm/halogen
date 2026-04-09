package halogen.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

internal actual suspend fun loadPixels(
    url: String,
    imageLoader: ImageLoader,
    context: PlatformContext,
): PixelData? {
    val request = ImageRequest.Builder(context)
        .data(url)
        .build()
    val result = imageLoader.execute(request)
    if (result !is SuccessResult) return null

    return try {
        val bitmap = result.image.toBitmap()
        val width = bitmap.width
        val height = bitmap.height
        val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
        val bytes = bitmap.readPixels(info, width * 4, 0, 0)
            ?: return null

        val pixels = IntArray(width * height)
        for (i in pixels.indices) {
            val offset = i * 4
            val b = bytes[offset].toInt() and 0xFF
            val g = bytes[offset + 1].toInt() and 0xFF
            val r = bytes[offset + 2].toInt() and 0xFF
            val a = bytes[offset + 3].toInt() and 0xFF
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        PixelData(pixels, width, height)
    } catch (_: Exception) {
        null
    }
}
