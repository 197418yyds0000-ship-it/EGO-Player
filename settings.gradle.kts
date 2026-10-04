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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") {
            content {
                includeGroup("com.github.FaceOnLive")
                includeGroup("com.github.philburk")
                includeGroup("com.github.racra")
                includeGroup("com.github.tdlibx")
            }
        }
    }
}

rootProject.name = "PixelPlay"
include(":app")
include(":shared")
include(":wear")
include(":baselineprofile")
