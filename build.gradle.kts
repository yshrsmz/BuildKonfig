buildscript {
    repositories {
        mavenCentral()
        google()
        jcenter()
        gradlePluginPortal()
        maven { url = uri( "https://dl.bintray.com/kotlin/kotlinx") }
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
        maven { url = uri("https://dl.bintray.com/jetbrains/kotlin-native-dependencies") }
    }

    dependencies {
        classpath(com.codingfeline.buildkonfig.build.Dependencies.kotlinPlugin)
        classpath(com.codingfeline.buildkonfig.build.Dependencies.dokkaPlugin)
        classpath(com.codingfeline.buildkonfig.build.Dependencies.gradleVersionsPlugin)
        classpath(com.codingfeline.buildkonfig.build.Dependencies.pluginPublishPlugin)
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.27.0"
}


val GROUP: String by project
val VERSION_NAME: String by project

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven { url = uri("https://dl.bintray.com/kotlin/kotlinx") }
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
    }

    group = GROUP
    version = VERSION_NAME
}

tasks.wrapper {
    gradleVersion = "5.6"
    distributionType = Wrapper.DistributionType.ALL
}
