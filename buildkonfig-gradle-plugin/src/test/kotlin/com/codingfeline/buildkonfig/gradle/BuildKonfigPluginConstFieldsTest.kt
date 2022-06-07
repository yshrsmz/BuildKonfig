package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BuildKonfigPluginConstFieldsTest {

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

    private val buildFileMPPConfig = """
        |kotlin {
        |  jvm()
        |  js(IR) {
        |    browser()
        |    nodejs()
        |  }
        |}
    """.trimMargin()

    @Before
    fun setup() {
        buildFile = projectDir.newFile("build.gradle.kts")
        settingFile = projectDir.newFile("settings.gradle")
        settingFile.writeText(settingsGradle)
    }

    @Test
    fun `const values for expect object contains suppress annotation`() {
        buildFile.writeText(
            """
            |import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |   
            |   defaultConfigs {
            |       buildConfigConstField(Type.STRING, "foo", "defaultValue")
            |   }
            |   targetConfigs {
            |       create("js") {
            |           buildConfigConstField(Type.STRING, "foo", "jsValue")
            |       }
            |   }
            |}
            |
            |$buildFileMPPConfig
            """.trimMargin()
        )

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

        val commonResult = File(buildDir, "commonMain/com/example/BuildKonfig.kt")
        Truth.assertThat(commonResult.readText()).apply {
            contains("@Suppress(\"CONST_VAL_WITHOUT_INITIALIZER\")")
            contains("const val foo: String")
        }

        val jvmResult = File(buildDir, "jvmMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jvmResult.readText())
            .contains("const val foo: String = \"defaultValue\"")

        val jsResult = File(buildDir, "jsMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jsResult.readText())
            .contains("const val foo: String = \"jsValue\"")
    }
}