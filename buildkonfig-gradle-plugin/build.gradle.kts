import com.codingfeline.buildkonfig.buildsrc.Dependencies
import com.codingfeline.buildkonfig.buildsrc.Versions

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val POM_URL: String by project
val POM_DESCRIPTION: String by project
val POM_NAME: String by project

pluginBundle {
    website = POM_URL
    vcsUrl = "https://github.com/yshrsmz/BuildKonfig.git"
    description = POM_DESCRIPTION
    tags = listOf("BuildConfig", "Kotlin", "Kotlin Multiplatform")

    plugins {
        create("buildKonfig") {
            displayName = POM_NAME
        }
    }
}

gradlePlugin {
    plugins {
        register("buildKonfig") {
            id = "com.codingfeline.buildkonfig"
            implementationClass = "com.codingfeline.buildkonfig.gradle.BuildKonfigPlugin"
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
    implementation(kotlin("stdlib-jdk8", Versions.kotlin))

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

//apply(from = "$rootDir/gradle/gradle-mvn-push.gradle")
apply(from = "$rootDir/gradle/maven-publish.gradle")