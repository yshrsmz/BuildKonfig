import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    @Suppress("DSL_SCOPE_VIOLATION") // See also, https://github.com/gradle/gradle/issues/22797#issuecomment-1517046458
    run {
        alias(libs.plugins.kotlin.jvm) apply false
        alias(libs.plugins.dokka) apply false
        alias(libs.plugins.pluginPublish) apply false
        alias(libs.plugins.mavenPublish) apply false
        alias(libs.plugins.versions)
    }
}


val GROUP: String by project
val VERSION_NAME: String by project

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    tasks.withType(JavaCompile::class.java) {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    group = GROUP
    version = VERSION_NAME
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.buildDir)
}

tasks.wrapper {
    gradleVersion = libs.versions.gradle.get()
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
