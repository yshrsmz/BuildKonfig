import com.codingfeline.buildkonfig.buildsrc.Dependencies
import com.codingfeline.buildkonfig.buildsrc.Versions

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
}

sourceSets {
    main {
        java.srcDir("src/generated/kotlin")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8", Versions.kotlin))
    implementation(Dependencies.kotlinPoet)
}

tasks.create("pluginVersion") {
    val outputDir = file("src/generated/kotlin")

    inputs.property("version", version)
    outputs.dir(outputDir)

    doLast {
        val versionFile = file("$outputDir/com/codingfeline/buildkonfig/Version.kt")
        versionFile.parentFile.mkdirs()
        versionFile.writeText(
            """
                |// Generated file. Do not edit!
                |package com.codingfeline.buildkonfig
                |
                |val VERSION = "${project.version}"
            """.trimMargin()
        )
    }
}

tasks.getByName("compileKotlin").dependsOn("pluginVersion")

tasks.compileKotlin {
    kotlinOptions.jvmTarget = Versions.jvmTarget
}
tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = Versions.jvmTarget
}

apply(from = "$rootDir/gradle/maven-publish.gradle")
