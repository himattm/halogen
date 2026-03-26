package me.mmckenna.halogen.sample

actual fun getEnvVar(name: String): String? = System.getenv(name)
