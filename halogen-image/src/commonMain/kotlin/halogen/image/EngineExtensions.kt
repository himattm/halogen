package halogen.image

import coil3.ImageLoader
import coil3.PlatformContext
import halogen.engine.HalogenEngine
import halogen.engine.HalogenResult

/**
 * Extract colors from raw pixels and resolve a theme via LLM with palette context.
 *
 * This overload is pure common code and does **not** require Coil.
 *
 * @param key       Theme cache key.
 * @param pixels    ARGB-packed pixel array (length = [width] * [height]).
 * @param width     Image width in pixels.
 * @param height    Image height in pixels.
 * @param maxColors Maximum dominant colors to extract (default 6).
 * @return A [HalogenResult] describing the outcome.
 */
public suspend fun HalogenEngine.resolveImage(
    key: String,
    pixels: IntArray,
    width: Int,
    height: Int,
    maxColors: Int = 6,
): HalogenResult {
    val colors = ImageQuantizer.extract(pixels, width, height, maxColors)
    return resolve(key, colors.toHint())
}

/**
 * Resolve a theme from an image URL using Coil.
 *
 * Uses Coil to load the image (leveraging its cache when the image was already
 * displayed), extracts dominant colors via [ImageQuantizer], and resolves a
 * theme through the LLM with the palette as context. The URL is used as the
 * theme cache key.
 *
 * @param url         The image URL to load.
 * @param imageLoader A configured Coil [ImageLoader] instance.
 * @param context     Platform context — on Android this is `android.content.Context`,
 *                    on other platforms use `PlatformContext.INSTANCE`.
 * @param maxColors   Maximum dominant colors to extract (default 6).
 * @return A [HalogenResult] describing the outcome, or
 *         [HalogenResult.Unavailable] if the image cannot be loaded.
 */
public suspend fun HalogenEngine.resolveImage(
    url: String,
    imageLoader: ImageLoader,
    context: PlatformContext,
    maxColors: Int = 6,
): HalogenResult {
    val data = loadPixels(url, imageLoader, context) ?: return HalogenResult.Unavailable
    return resolveImage(url, data.pixels, data.width, data.height, maxColors)
}
