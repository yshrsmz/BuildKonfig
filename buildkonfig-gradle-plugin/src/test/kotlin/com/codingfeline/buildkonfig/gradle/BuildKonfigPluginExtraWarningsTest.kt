package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Regression tests for https://github.com/yshrsmz/BuildKonfig/issues/365 — generated code must not
 * emit Kotlin `extraWarnings` diagnostics, which break builds using `allWarningsAsErrors`.
 */
class BuildKonfigPluginExtraWarningsTest : BaseGradlePluginTest() {

    override val buildFileName: String = "build.gradle.kts"

    private val buildFileHeader = buildFileHeaderKts("kotlin-multiplatform")

    private val strictCompilerOptions = """
        |  compilerOptions {
        |    extraWarnings.set(true)
        |    allWarningsAsErrors.set(true)
        |  }
    """.trimMargin()

    @Test
    fun `common object compiles with extraWarnings and allWarningsAsErrors`() {
        buildFile.writeText(
            """
            |import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField(Type.STRING, "VERSION_NAME", "1.0.0", const = true)
            |       buildConfigField(Type.INT, "VERSION_CODE", "42")
            |       buildConfigField(Type.STRING, "OPTIONAL", null, nullable = true)
            |   }
            |}
            |
            |kotlin {
            |  jvm()
            |$strictCompilerOptions
            |}
            """.trimMargin()
        )

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("compileKotlinJvm", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        val content = buildKonfigFile(buildDir, "commonMain", "com.example").readText()
        assertThat(content).contains("REDUNDANT_VISIBILITY_MODIFIER")
    }

    @Test
    fun `expect and actual objects compile with extraWarnings and allWarningsAsErrors`() {
        buildFile.writeText(
            """
            |import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |   exposeObjectWithName = "ExposedBuildKonfig"
            |
            |   defaultConfigs {
            |       buildConfigField(Type.STRING, "name", "defaultValue")
            |   }
            |   targetConfigs {
            |       create("jvm") {
            |           buildConfigField(Type.STRING, "name", "jvmValue")
            |       }
            |   }
            |}
            |
            |kotlin {
            |  jvm()
            |$strictCompilerOptions
            |}
            """.trimMargin()
        )

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("compileKotlinJvm", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        listOf("commonMain", "jvmMain").forEach { sourceSet ->
            val content = buildKonfigFile(buildDir, sourceSet, "com.example", "ExposedBuildKonfig").readText()
            assertThat(content).apply {
                contains("REDUNDANT_VISIBILITY_MODIFIER")
                contains("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
            }
        }
    }
}
