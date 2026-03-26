plugins {
    id("halogen.kmp-library")
    id("halogen.publishing")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":halogen-core"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
    }
}

android {
    namespace = "me.mmckenna.halogen.compose"
}

mavenPublishing {
    pom {
        name.set("Halogen Compose")
        description.set("Compose Multiplatform UI components for Halogen on-device AI inference")
    }
}
