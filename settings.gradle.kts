pluginManagement {
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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "wm-suite"

// Android libraries the WM apps share. Apps consume this as a git submodule through
// includeBuild, so `com.wasimaster.wmsuite:<module>` resolves to the source here.
include(":icons")
include(":ui-core")
include(":theme")
include(":settings-ui")
include(":sinks")
include(":kdeconnect")
