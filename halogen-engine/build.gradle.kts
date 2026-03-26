plugins {
    id("halogen.kmp-library")
    id("halogen.publishing")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":halogen-core"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.datastore.preferences)
        }

        jvmMain.dependencies {
            implementation(libs.androidx.datastore.preferences)
        }

        iosMain.dependencies {
            implementation(libs.androidx.datastore.preferences)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "me.mmckenna.halogen.engine"
}

mavenPublishing {
    pom {
        name.set("Halogen Engine")
        description.set("Persistence and orchestration engine for Halogen on-device AI inference")
    }
}
