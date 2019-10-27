pluginManagement {
    repositories {
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }

        mavenCentral()

        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
}
rootProject.name = "BuildKonfig"

include("buildkonfig-compiler")
include("buildkonfig-gradle-plugin")
