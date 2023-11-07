package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BuildKonfigPluginKotlinDSLFlavorTest {

    @get:Rule
    val projectDir = TemporaryFolder()

    lateinit var buildFile: File

    lateinit var settingFile: File

    private val buildFileHeader = """
        |plugins {
        |    id("kotlin-multiplatform")
        |    id("com.codingfeline.buildkonfig")
        |}
        |
        |repositories {
        |   mavenCentral()
        |}
        |
    """.trimMargin()

    private val buildFileKMPConfig = """
        |kotlin {
        |  jvm()
        |  js(IR) {
        |    browser()
        |    nodejs()
        |  }
        |  iosX64()
        |  iosArm64()
        |  iosSimulatorArm64()
        |}
    """.trimMargin()

    @Before
    fun setup() {
        buildFile = projectDir.newFile("build.gradle.kts")
        settingFile = projectDir.newFile("settings.gradle")
        settingFile.writeText(settingsGradle)
    }

    @Test
    fun `Flavored targetConfigs overwrites default targetConfigs`() {
        buildFile.writeText(
            """
            |import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |   
            |   defaultConfigs {
            |       buildConfigField(Type.STRING, "value", "defaultValue")
            |   }
            |   targetConfigs {
            |       create("js") {
            |           buildConfigField(Type.STRING,"value", "foobar")
            |           buildConfigField(type = Type.STRING, name = "overwrittenValue", value = "defaultJsValue")
            |       }
            |   }
            |   targetConfigs("dev") {
            |       create("js") {
            |           buildConfigField(type = Type.STRING, name = "overwrittenValue", value = "devJsValue")
            |       }
            |   }
            |}
            |
            |$buildFileKMPConfig
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
            .apply {
                contains("foobar")
                contains("devJsValue")
            }

        val iosResult = File(buildDir, "iosX64Main/com/example/BuildKonfig.kt")
        Truth.assertThat(iosResult.readText())
            .contains("defaultValue")
    }
}
