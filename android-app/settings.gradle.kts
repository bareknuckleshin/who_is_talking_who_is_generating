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
    }
}

rootProject.name = "WhoIsTalkingAndroid"
include(
    ":app",
    ":core:ui",
    ":core:domain",
    ":core:data",
    ":feature:session"
)
