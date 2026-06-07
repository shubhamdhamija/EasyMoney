pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // MPAndroidChart is available via JitPack
        maven("https://jitpack.io")
    }
}

rootProject.name = "EasyMoney"
include(":app")
