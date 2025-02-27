import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.mavenPublish)
}

val outputDir = layout.projectDirectory.dir("src/generated/kotlin")

sourceSets {
    main {
        java.srcDir(outputDir)
    }
}

tasks.named<Delete>("clean") {
    delete(outputDir)
}

dependencies {
    implementation(libs.kotlinpoet)
}

tasks.register("pluginVersion") {
    inputs.property("version", version)
    outputs.file(outputDir.file("com/codingfeline/buildkonfig/Version.kt"))

    doLast {
        val versionFile = outputs.files.singleFile
        versionFile.parentFile.mkdirs()
        versionFile.writeText(
            """
                |// Generated file. Do not edit!
                |package com.codingfeline.buildkonfig
                |
                |const val VERSION = "${inputs.properties["version"]}"
            """.trimMargin()
        )
    }
}

afterEvaluate {
    tasks.named("compileKotlin").configure { dependsOn("pluginVersion") }
    tasks.named("dokkaGeneratePublicationHtml").configure { dependsOn("pluginVersion") }
    tasks.named("sourcesJar").configure { dependsOn("pluginVersion") }
    tasks.named("kotlinSourcesJar").configure { dependsOn("pluginVersion") }
}

tasks.compileKotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_1_8) }
}
tasks.compileTestKotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_1_8) }
}

apply(from = "$rootDir/gradle/maven-publish.gradle")
