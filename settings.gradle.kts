pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "Halogen"
include(
    ":halogen-core",
    ":halogen-compose",
    ":halogen-engine",
    ":halogen-cache-room",
    ":halogen-image",
    ":halogen-provider-nano",
    ":halogen-chromecast",
    ":sample",
    ":sample-shared",
    ":sample-chromecast-sender",
)
