plugins {
    id("halogen.kmp-library")
    id("halogen.publishing")
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
            implementation(project(":halogen-core"))
            implementation(libs.kotlinx.coroutines.core)
        }

        roomMain.dependencies {
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}

android {
    namespace = "me.mmckenna.halogen.engine"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dokka {
    dokkaSourceSets.configureEach {
        // Only document commonMain — all public API lives there.
        // The roomMain intermediate source set has a metadata compilation issue
        // (expect/actual for RoomDatabaseConstructor) and is all internal anyway.
        if (name != "commonMain") {
            suppress.set(true)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

mavenPublishing {
    pom {
        name.set("Halogen Engine")
        description.set("Persistence and orchestration engine for Halogen on-device AI inference")
    }
}
