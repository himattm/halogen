package me.mmckenna.halogen.sample

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKStringFromUtf8
import platform.Foundation.NSBundle
import platform.posix.getenv

private val plistKeyMap = mapOf(
    "OPENAI_API_KEY" to "OpenAIAPIKey",
)

@OptIn(ExperimentalForeignApi::class)
actual fun getEnvVar(name: String): String? {
    // Try environment variable first (works with SIMCTL_CHILD_ prefix)
    getenv(name)?.toKStringFromUtf8()?.let { return it }
    // Fall back to Info.plist (populated from local.properties via xcconfig)
    val plistKey = plistKeyMap[name] ?: return null
    return (NSBundle.mainBundle.infoDictionary?.get(plistKey) as? String)?.takeIf { it.isNotBlank() }
}
