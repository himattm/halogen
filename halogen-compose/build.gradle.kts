plugins {
    id("halogen.kmp-library")
    id("halogen.publishing")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

@OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
@Suppress("OPT_IN_USAGE")
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":halogen-core"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.animation)
            implementation(compose.ui)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(compose.uiTest)
        }

        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
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
