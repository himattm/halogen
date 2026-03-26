package me.mmckenna.halogen.sample

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKStringFromUtf8
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
actual fun getEnvVar(name: String): String? = getenv(name)?.toKStringFromUtf8()
