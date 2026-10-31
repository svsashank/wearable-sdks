pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        @Suppress("DEPRECATION")
        jcenter()   // ring SDK needs okhttputils + fastjson converter, only published there (read-only, still served)
    }
}
rootProject.name = "smriti"
include(":app")
