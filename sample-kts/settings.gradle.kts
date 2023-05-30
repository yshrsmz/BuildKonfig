pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven { url = uri("../build/localMaven") }
        mavenLocal()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "sample-kts"

include("multiplatform")
