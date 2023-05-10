package com.codingfeline.buildkonfig.gradle

import org.junit.rules.TemporaryFolder

val androidManifest = """
        |<?xml version="1.0" encoding="utf-8"?>
        |<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
    """.trimMargin()

fun createAndroidManifest(projectDir: TemporaryFolder, sourceSetName: String = "androidMain") {
    projectDir.newFolder("src", sourceSetName)
    val androidManifestFile = projectDir.newFile("src/$sourceSetName/AndroidManifest.xml")
    androidManifestFile.writeText(androidManifest)
}


val settingsGradle = """
            |pluginManagement {
            |   resolutionStrategy {
            |       eachPlugin {
            |           if (requested.id.id == "kotlin-multiplatform") {
            |               useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}{requested.version}")
            |           }
            |       }
            |   }
            |
            |   repositories {
            |       mavenCentral()
            |       jcenter()
            |       maven { url 'https://plugins.gradle.org/m2/' }
            |   }
            |}
        """.trimMargin()
