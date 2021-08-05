import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

buildscript {
    repositories {
        mavenCentral()
        google()
        jcenter()
        gradlePluginPortal()
        maven { url = uri("https://dl.bintray.com/kotlin/kotlinx") }
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
        maven { url = uri("https://dl.bintray.com/jetbrains/kotlin-native-dependencies") }
    }

    dependencies {
        classpath(com.codingfeline.buildkonfig.buildsrc.Dependencies.kotlinPlugin)
        classpath(com.codingfeline.buildkonfig.buildsrc.Dependencies.dokkaPlugin)
        classpath(com.codingfeline.buildkonfig.buildsrc.Dependencies.gradleVersionsPlugin)
        classpath(com.codingfeline.buildkonfig.buildsrc.Dependencies.pluginPublishPlugin)
        classpath(com.codingfeline.buildkonfig.buildsrc.Dependencies.mavenPublishPlugin)
    }
}

plugins {
    id("com.github.ben-manes.versions") version com.codingfeline.buildkonfig.buildsrc.Versions.benManesVersionsPlugin
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

tasks.register("clean", Delete::class.java) {
    delete(rootProject.buildDir)
}

tasks.wrapper {
    gradleVersion = com.codingfeline.buildkonfig.buildsrc.Versions.gradle
    distributionType = Wrapper.DistributionType.ALL
}

// https://github.com/ben-manes/gradle-versions-plugin
fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}