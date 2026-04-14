package halogen.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap

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

    val bitmap = result.image.toBitmap()
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    return PixelData(pixels, width, height)
}
