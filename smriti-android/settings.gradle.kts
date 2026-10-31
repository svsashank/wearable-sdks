pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        // The ring SDK needs okhttputils + a fastjson converter, old JCenter-only libraries.
        // The vendor demo resolves them from this public mirror; build-time downloads only.
        maven("https://maven.aliyun.com/repository/public") {
            content { includeGroup("com.zhy"); includeGroup("org.ligboy.retrofit2") }
        }
    }
}
rootProject.name = "smriti"
include(":app")
