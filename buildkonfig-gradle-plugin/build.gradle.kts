import com.codingfeline.buildkonfig.buildsrc.Dependencies
import com.codingfeline.buildkonfig.buildsrc.Versions

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.pluginPublish)
    id("java-gradle-plugin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val POM_URL: String by project
val POM_DESCRIPTION: String by project
val POM_NAME: String by project

gradlePlugin {
    plugins {
        create("buildKonfig") {
            id = "com.codingfeline.buildkonfig"
            implementationClass = "com.codingfeline.buildkonfig.gradle.BuildKonfigPlugin"
        }
    }
}

pluginBundle {
    website = POM_URL
    vcsUrl = "https://github.com/yshrsmz/BuildKonfig.git"
    description = POM_DESCRIPTION
    tags = listOf("BuildConfig", "Kotlin", "Kotlin Multiplatform")

    (plugins) {
        "buildKonfig" {
            displayName = POM_NAME
        }
    }
}


val fixtureClasspath by configurations.creating

// Append any extra dependencies to the test fixtures via a custom configuration classpath. This
// allows us to apply additional plugins in a fixture while still leveraging dependency resolution
// and de-duplication semantics.
tasks.pluginUnderTestMetadata {
    pluginClasspath.from(fixtureClasspath)
}

dependencies {
    implementation(project(":buildkonfig-compiler"))
//    implementation(kotlin("stdlib-jdk8", Versions.kotlin))

    compileOnly(gradleApi())
    implementation(kotlin("gradle-plugin", Versions.kotlin))

    testImplementation(Dependencies.junit)
    testImplementation(Dependencies.truth)

    fixtureClasspath(kotlin("gradle-plugin", Versions.kotlin))
    fixtureClasspath(Dependencies.androidPlugin)
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = Versions.jvmTarget
}
tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = Versions.jvmTarget
}

apply(from = "$rootDir/gradle/maven-publish.gradle")
