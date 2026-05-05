package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.rules.TemporaryFolder
import java.io.File

const val BUILDKONFIG_BUILD_DIR = "build/buildkonfig"

/**
 * Returns the (freshly cleaned) `build/buildkonfig` directory under [TemporaryFolder.getRoot].
 * Tests assert on files inside this directory, so they need a clean slate per run.
 */
fun TemporaryFolder.buildKonfigDir(): File =
    File(root, BUILDKONFIG_BUILD_DIR).also { it.deleteRecursively() }

/**
 * Standard `GradleRunner` configured against [projectDir] with the plugin under test on
 * the classpath. Tests should chain `.withArguments(...)` and `.build()` /
 * `.buildAndFail()` as usual.
 */
fun gradleRunner(projectDir: TemporaryFolder): GradleRunner =
    GradleRunner.create()
        .withProjectDir(projectDir.root)
        .withPluginClasspath()

/**
 * Asserts that the build output contains `BUILD SUCCESSFUL`. Returns the receiver so the
 * call can be chained with further assertions on the same [BuildResult].
 */
fun BuildResult.assertBuildSuccessful(): BuildResult {
    assertThat(output).contains("BUILD SUCCESSFUL")
    return this
}

/**
 * Resolves the generated `BuildKonfig.kt` (or [objectName]`.kt`) for a given
 * [sourceSetName] and [packageName] under [buildKonfigDir].
 */
fun buildKonfigFile(
    buildKonfigDir: File,
    sourceSetName: String,
    packageName: String,
    objectName: String = "BuildKonfig",
): File = File(
    buildKonfigDir,
    "$sourceSetName/${packageName.replace('.', '/')}/$objectName.kt",
)

val androidManifest = """
        |<?xml version="1.0" encoding="utf-8"?>
        |<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
    """.trimMargin()

fun createAndroidManifest(projectDir: TemporaryFolder, sourceSetName: String = "androidMain") {
    projectDir.newFolder("src", sourceSetName)
    val androidManifestFile = projectDir.newFile("src/$sourceSetName/AndroidManifest.xml")
    androidManifestFile.writeText(androidManifest)
}


/**
 * Build script header — `plugins { ... }` and a `repositories { google() mavenCentral() }`
 * block — in Groovy DSL syntax (`build.gradle`). The Kotlin plugin is applied first, then
 * any [additionalPlugins] in order, then `com.codingfeline.buildkonfig` last.
 *
 * `google()` is included unconditionally so the same header works for AGP-based fixtures;
 * for fixtures that don't touch Android the extra repository is harmless.
 */
fun buildFileHeader(kotlinPluginId: String, vararg additionalPlugins: String): String {
    val pluginIds = listOf(kotlinPluginId) + additionalPlugins + "com.codingfeline.buildkonfig"
    val pluginLines = pluginIds.joinToString("\n") { "            |    id '$it'" }
    return """
            |plugins {
$pluginLines
            |}
            |
            |repositories {
            |   google()
            |   mavenCentral()
            |}
            |
        """.trimMargin()
}

/**
 * Kotlin DSL counterpart of [buildFileHeader] — `plugins { ... }` and a
 * `repositories { google() mavenCentral() }` block in `build.gradle.kts` syntax.
 */
fun buildFileHeaderKts(kotlinPluginId: String, vararg additionalPlugins: String): String {
    val pluginIds = listOf(kotlinPluginId) + additionalPlugins + "com.codingfeline.buildkonfig"
    val pluginLines = pluginIds.joinToString("\n") { "            |    id(\"$it\")" }
    return """
            |plugins {
$pluginLines
            |}
            |
            |repositories {
            |   google()
            |   mavenCentral()
            |}
            |
        """.trimMargin()
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
            |       maven { url 'https://plugins.gradle.org/m2/' }
            |   }
            |}
        """.trimMargin()
