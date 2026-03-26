plugins {
    id("halogen.kmp-library")
    id("halogen.publishing")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "me.mmckenna.halogen.core"
}

mavenPublishing {
    pom {
        name.set("Halogen Core")
        description.set("Core primitives and contracts for Halogen on-device AI inference")
    }
}
