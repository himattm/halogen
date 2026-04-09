plugins {
    id("halogen.kmp-library")
    id("halogen.publishing")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":halogen-core"))
            implementation(project(":halogen-engine"))
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
