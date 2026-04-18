plugins {
    id("halogen.kmp-library")
    id("halogen.publishing")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":halogen-core"))
            api(project(":halogen-engine"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "me.mmckenna.halogen.chromecast"
}

mavenPublishing {
    pom {
        name.set("Halogen Chromecast")
        description.set(
            "Wire protocol and orchestration for migrating Halogen palettes to a " +
                "Chromecast receiver. SDK-free; consumer supplies the Cast transport.",
        )
    }
}
