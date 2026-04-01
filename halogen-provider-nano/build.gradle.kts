plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.dokka)
    id("halogen.publishing")
}

kotlin {
    explicitApi()
}

android {
    namespace = "me.mmckenna.halogen.provider.nano"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":halogen-core"))
    implementation(libs.mlkit.genai.prompt)
    implementation(libs.kotlinx.coroutines.android)
}

mavenPublishing {
    pom {
        name.set("Halogen Provider — Gemini Nano")
        description.set("Gemini Nano on-device AI provider for Halogen")
    }
}
