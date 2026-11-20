pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
            credentials {
                username = providers.gradleProperty("jitpackUsername").orNull
                password = providers.gradleProperty("jitpackPassword").orNull
            }
        }
    }
}

rootProject.name = "bridge-app"
include(":app")
