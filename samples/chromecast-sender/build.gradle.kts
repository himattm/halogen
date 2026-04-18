plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "halogen.samples.chromecast"
    compileSdk = 36

    defaultConfig {
        applicationId = "halogen.samples.chromecast"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        val castAppId = providers.gradleProperty("HALOGEN_CAST_APP_ID")
            .orElse(providers.environmentVariable("HALOGEN_CAST_APP_ID"))
            .getOrElse("CC1AD845") // Google's default media receiver — placeholder; register your own.
        buildConfigField("String", "CAST_APP_ID", "\"$castAppId\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":halogen-core"))
    implementation(project(":halogen-engine"))
    implementation(project(":halogen-chromecast"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.play.services.cast.framework)
    implementation(libs.androidx.mediarouter)

    debugImplementation(libs.androidx.ui.tooling)
}
