plugins {
    id("halogen.kmp-library")
    id("halogen.publishing")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    sourceSets {
        val roomMain by creating {
            dependsOn(commonMain.get())
        }

        val iosMain by getting {
            dependsOn(roomMain)
        }

        androidMain.get().dependsOn(roomMain)
        jvmMain.get().dependsOn(roomMain)

        commonMain.dependencies {
            api(project(":halogen-core"))
            api(project(":halogen-engine"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }

        roomMain.dependencies {
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}

android {
    namespace = "me.mmckenna.halogen.cache.room"
}

dependencies {
    listOf("kspAndroid", "kspJvm", "kspIosArm64", "kspIosSimulatorArm64").forEach {
        add(it, libs.androidx.room.compiler)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dokka {
    dokkaSourceSets.configureEach {
        if (name.contains("room", ignoreCase = true)) {
            suppress.set(true)
        }
    }
}

mavenPublishing {
    pom {
        name.set("Halogen Cache — Room")
        description.set("Optional Room KMP persistent cache for Halogen themes")
    }
}
