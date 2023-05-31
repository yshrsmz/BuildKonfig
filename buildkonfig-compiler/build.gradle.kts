plugins {
    @Suppress("DSL_SCOPE_VIOLATION") // See also, https://github.com/gradle/gradle/issues/22797#issuecomment-1517046458
    run {
        alias(libs.plugins.kotlin.jvm)
        alias(libs.plugins.mavenPublish)
    }
}

sourceSets {
    main {
        java.srcDir("src/generated/kotlin")
    }
}

dependencies {
    implementation(libs.kotlinpoet)
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

afterEvaluate {
    tasks.named("compileKotlin").configure { dependsOn("pluginVersion") }
    tasks.named("dokkaHtml").configure { dependsOn("pluginVersion") }
    tasks.named("kotlinSourcesJar").configure { dependsOn("pluginVersion") }
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = libs.versions.jvmTarget.get()
}
tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = libs.versions.jvmTarget.get()
}

apply(from = "$rootDir/gradle/maven-publish.gradle")
