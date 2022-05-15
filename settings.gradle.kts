pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}
rootProject.name = "BuildKonfig"

include("buildkonfig-compiler")
include("buildkonfig-gradle-plugin")
