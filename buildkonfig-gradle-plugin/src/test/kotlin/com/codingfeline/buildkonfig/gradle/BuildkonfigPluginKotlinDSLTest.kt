package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BuildkonfigPluginKotlinDSLTest {

    @get:Rule
    val projectDir = TemporaryFolder()

    lateinit var buildFile: File

    lateinit var settingFile: File

    @Before
    fun setup() {
        buildFile = projectDir.newFile("build.gradle.kts")
        settingFile = projectDir.newFile("settings.gradle")
        settingFile.writeText(settingsGradle)
    }

    @Test
    fun `issue 50 - js(IR) target`() {
        buildFile.writeText(
            """
            |import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
            |import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
            |
            |plugins {
            |    kotlin("multiplatform")
            |    id("com.android.library")
            |    id("com.codingfeline.buildkonfig")
            |}
            |
            |kotlin {
            |    androidTarget()
            |
            |    iosX64()
            |    iosArm64()
            |    iosSimulatorArm64()
            |
            |    js(IR) {
            |        browser()
            |    }
            |    
            |    applyDefaultHierarchyTemplate()
            |
            |    sourceSets {
            |        val commonMain by getting
            |
            |        val androidMain by getting
            |
            |        val iosMain by getting
            |
            |        val jsMain by getting
            |    }
            |}
            |
            |configure<com.codingfeline.buildkonfig.gradle.BuildKonfigExtension> {
            |    packageName = "com.sample.buildkonfig.issues"
            |
            |    defaultConfigs {
            |        buildConfigField(Type.STRING, "VERSION", "mysupersecretpassword")
            |    }
            |}
            |
            |android {
            |    compileSdkVersion(30)
            |    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
            |    defaultConfig {
            |        minSdkVersion(21)
            |        targetSdkVersion(30)
            |    }
            |    
            |    namespace = "com.sample"
            |}
            """.trimMargin()
        )

        createAndroidManifest(projectDir)

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()

//        println(result.output)

        assertThat(result.output).contains("BUILD SUCCESSFUL")

        val commonResult = File(buildDir, "commonMain/com/sample/buildkonfig/issues/BuildKonfig.kt")
        assertThat(commonResult.exists()).isTrue()

        val androidResult = File(buildDir, "androidMain/com/sample/buildkonfig/issues/BuildKonfig.kt")
        assertThat(androidResult.exists()).isFalse()

        val jsResult = File(buildDir, "jsMain/com/sample/buildkonfig/issues/BuildKonfig.kt")
        assertThat(jsResult.exists()).isFalse()
    }
}
