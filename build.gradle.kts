plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.binary.compatibility.validator)
}

dokka {
    moduleName.set("Halogen")
    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
    }
}

dependencies {
    dokka(project(":halogen-core"))
    dokka(project(":halogen-engine"))
    dokka(project(":halogen-cache-room"))
    dokka(project(":halogen-compose"))
    dokka(project(":halogen-image"))
    dokka(project(":halogen-provider-nano"))
    dokka(project(":halogen-chromecast"))
}

apiValidation {
    // TODO(chromecast): run `./gradlew :halogen-chromecast:apiDump`, commit the
    // generated api/ files, and drop "halogen-chromecast" from this list before
    // the 0.2.0 release so binary compatibility is tracked.
    ignoredProjects += listOf("sample", "sample-shared", "sample-chromecast-sender", "halogen-chromecast")
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}
