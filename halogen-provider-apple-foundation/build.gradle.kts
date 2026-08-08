import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.dokka)
    id("halogen.publishing")
}

kotlin {
    explicitApi()

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val iosMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                api(project(":halogen-core"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        val iosTest by creating {
            dependsOn(commonTest.get())
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val iosArm64Test by getting { dependsOn(iosTest) }
        val iosSimulatorArm64Test by getting { dependsOn(iosTest) }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

mavenPublishing {
    pom {
        name.set("Halogen Provider — Apple Foundation Models")
        description.set("Apple Foundation Models on-device AI provider for Halogen (iOS 26+)")
    }
}
