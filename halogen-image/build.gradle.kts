plugins {
    id("halogen.kmp-library")
    id("halogen.publishing")
}

kotlin {
    sourceSets {
        val skikoMain by creating {
            dependsOn(commonMain.get())
        }

        jvmMain.get().dependsOn(skikoMain)

        val iosMain by getting {
            dependsOn(skikoMain)
        }

        val wasmJsMain by getting {
            dependsOn(skikoMain)
        }

        commonMain.dependencies {
            api(project(":halogen-core"))
            api(project(":halogen-engine"))
            implementation(libs.coil.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "me.mmckenna.halogen.image"
}

mavenPublishing {
    pom {
        name.set("Halogen Image")
        description.set("Image-to-theme color extraction for Halogen")
    }
}
