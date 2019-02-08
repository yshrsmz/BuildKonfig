package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BuildKonfigPluginFlavorTest {

    @get:Rule
    val projectDir = TemporaryFolder()

    lateinit var buildFile: File

    lateinit var settingFile: File

    private val buildFileHeader = """
        |plugins {
        |    id 'kotlin-multiplatform'
        |    id 'com.codingfeline.buildkonfig'
        |}
        |
        |repositories {
        |   mavenCentral()
        |}
        |
    """.trimMargin()

    private val buildFileMPPConfig = """
        |kotlin {
        |  jvm()
        |  js()
        |  iosX64('ios')
        |}
    """.trimMargin()

    @Before
    fun setup() {
        buildFile = projectDir.newFile("build.gradle")
        settingFile = projectDir.newFile("settings.gradle")
        settingFile.writeText(
            """
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
            |enableFeaturePreview("GRADLE_METADATA")
        """.trimMargin()
        )
    }

    @Test
    fun `flavor can be obtained from gradle properties file`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'value', 'defaultValue'
            |   }
            |   defaultConfigs("dev") {
            |       buildConfigField 'STRING', 'value', 'devDefaultValue'
            |   }
            |}
            |$buildFileMPPConfig
        """.trimMargin()
        )

        val propertyFile = projectDir.newFile("gradle.properties")
        propertyFile.writeText("buildkonfig.flavor=dev")

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val jvmResult = File(buildDir, "jvmMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jvmResult.readText())
            .contains("devDefaultValue")

        val jsResult = File(buildDir, "jsMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jsResult.readText())
            .contains("devDefaultValue")

        val iosResult = File(buildDir, "iosMain/com/example/BuildKonfig.kt")
        Truth.assertThat(iosResult.readText())
            .contains("devDefaultValue")
    }

    @Test
    fun `flavor can be overwritten by cli parameter`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'value', 'defaultValue'
            |   }
            |   defaultConfigs("dev") {
            |       buildConfigField 'STRING', 'value', 'devDefaultValue'
            |   }
            |   defaultConfigs("release") {
            |       buildConfigField 'STRING', 'value', 'releaseDefaultValue'
            |   }
            |}
            |$buildFileMPPConfig
        """.trimMargin()
        )

        val propertyFile = projectDir.newFile("gradle.properties")
        propertyFile.writeText("buildkonfig.flavor=dev")

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace", "-Pbuildkonfig.flavor=release")
            .build()

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val jvmResult = File(buildDir, "jvmMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jvmResult.readText())
            .contains("releaseDefaultValue")

        val jsResult = File(buildDir, "jsMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jsResult.readText())
            .contains("releaseDefaultValue")

        val iosResult = File(buildDir, "iosMain/com/example/BuildKonfig.kt")
        Truth.assertThat(iosResult.readText())
            .contains("releaseDefaultValue")
    }

    @Test
    fun `Default targetConfigs overwrite flavored defaultConfigs`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'value', 'defaultValue'
            |   }
            |   defaultConfigs("dev") {
            |       buildConfigField 'STRING', 'value', 'devDefaultValue'
            |   }
            |   targetConfigs {
            |       jvm {
            |           buildConfigField 'STRING', 'value', 'jvmDefaultValue'
            |       }
            |   }
            |}
            |$buildFileMPPConfig
        """.trimMargin()
        )

        val propertyFile = projectDir.newFile("gradle.properties")
        propertyFile.writeText("buildkonfig.flavor=dev")

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val jvmResult = File(buildDir, "jvmMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jvmResult.readText())
            .contains("jvmDefaultValue")

        val jsResult = File(buildDir, "jsMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jsResult.readText())
            .contains("devDefaultValue")

        val iosResult = File(buildDir, "iosMain/com/example/BuildKonfig.kt")
        Truth.assertThat(iosResult.readText())
            .contains("devDefaultValue")
    }

    @Test
    fun `Flavored targetConfigs overwrite flavored defaultConfigs`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'value', 'defaultValue'
            |   }
            |   defaultConfigs("dev") {
            |       buildConfigField 'STRING', 'value', 'devDefaultValue'
            |   }
            |   targetConfigs("dev") {
            |       js {
            |           buildConfigField 'STRING', 'value', 'devJsValue'
            |       }
            |   }
            |}
            |$buildFileMPPConfig
        """.trimMargin()
        )

        val propertyFile = projectDir.newFile("gradle.properties")
        propertyFile.writeText("buildkonfig.flavor=dev")

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val jvmResult = File(buildDir, "jvmMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jvmResult.readText())
            .contains("devDefaultValue")

        val jsResult = File(buildDir, "jsMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jsResult.readText())
            .contains("devJsValue")

        val iosResult = File(buildDir, "iosMain/com/example/BuildKonfig.kt")
        Truth.assertThat(iosResult.readText())
            .contains("devDefaultValue")
    }

    @Test
    fun `Flavored targetConfigs overwrite default targetConfigs`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'value', 'defaultValue'
            |   }
            |   targetConfigs {
            |       js {
            |           buildConfigField 'STRING', 'value', 'defaultJsValue'
            |       }
            |   }
            |   targetConfigs("dev") {
            |       js {
            |           buildConfigField 'STRING', 'value', 'devJsValue'
            |       }
            |   }
            |}
            |$buildFileMPPConfig
        """.trimMargin()
        )

        val propertyFile = projectDir.newFile("gradle.properties")
        propertyFile.writeText("buildkonfig.flavor=dev")

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val jvmResult = File(buildDir, "jvmMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jvmResult.readText())
            .contains("defaultValue")

        val jsResult = File(buildDir, "jsMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jsResult.readText())
            .contains("devJsValue")

        val iosResult = File(buildDir, "iosMain/com/example/BuildKonfig.kt")
        Truth.assertThat(iosResult.readText())
            .contains("defaultValue")
    }
}