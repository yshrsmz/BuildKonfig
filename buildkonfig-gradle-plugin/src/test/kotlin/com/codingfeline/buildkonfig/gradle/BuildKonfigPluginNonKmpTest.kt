package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class BuildKonfigPluginNonKmpTest : BaseGradlePluginTest() {

    private val jvmBuildFileHeader = buildFileHeader("org.jetbrains.kotlin.jvm")

    @Test
    fun `Applying plugin to a Kotlin JVM project generates a single concrete object`() {
        buildFile.writeText(
            """
            |$jvmBuildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.sample"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'env', 'production'
            |       buildConfigField 'INT', 'version', '42'
            |   }
            |}
            """.trimMargin()
        )

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        val generated = buildKonfigFile(buildDir, "main", "com.sample")
        assertThat(generated.exists()).isTrue()
        val content = generated.readText()
        assertThat(content).contains("internal object BuildKonfig")
        assertThat(content).contains("public val env: String = \"production\"")
        assertThat(content).contains("public val version: Int = 42")
        // No expect/actual generation for non-KMP projects.
        assertThat(content).doesNotContain("expect ")
        assertThat(content).doesNotContain("actual ")
    }

    @Test
    fun `compileKotlin picks up the generated source on a Kotlin JVM project`() {
        buildFile.writeText(
            """
            |$jvmBuildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.sample"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'env', 'production'
            |   }
            |}
            """.trimMargin()
        )

        // Write a source file that references the generated BuildKonfig object so that
        // the kotlin compilation will fail unless the generated source dir was wired
        // into the JVM main source set.
        val srcDir = projectDir.newFolder("src", "main", "kotlin", "com", "sample")
        File(srcDir, "Usage.kt").writeText(
            """
            package com.sample

            fun consumeEnv(): String = BuildKonfig.env
            """.trimIndent()
        )

        gradleRunner(projectDir)
            .withArguments("compileKotlin", "--stacktrace")
            .build()
            .assertBuildSuccessful()
    }

    @Test
    fun `flavor selection works on a Kotlin JVM project`() {
        buildFile.writeText(
            """
            |$jvmBuildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.sample"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'env', 'production'
            |   }
            |   defaultConfigs("staging") {
            |       buildConfigField 'STRING', 'env', 'staging'
            |   }
            |}
            """.trimMargin()
        )

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "-Pbuildkonfig.flavor=staging", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        val generated = buildKonfigFile(buildDir, "main", "com.sample")
        assertThat(generated.readText()).contains("public val env: String = \"staging\"")
    }

    @Test
    fun `Applying plugin to a Kotlin JS project generates a single concrete object`() {
        buildFile.writeText(
            """
            |plugins {
            |    id 'org.jetbrains.kotlin.js'
            |    id 'com.codingfeline.buildkonfig'
            |}
            |
            |repositories {
            |   mavenCentral()
            |}
            |
            |kotlin {
            |   js {
            |       browser()
            |       nodejs()
            |       binaries.executable()
            |   }
            |}
            |
            |buildkonfig {
            |   packageName = "com.sample"
            |   exposeObjectWithName = "BuildKonfig"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'env', 'production'
            |   }
            |}
            """.trimMargin()
        )

        // kotlin-js requires a main entry point for the executable.
        val srcDir = projectDir.newFolder("src", "main", "kotlin")
        File(srcDir, "Main.kt").writeText("fun main() {}")

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        val generated = buildKonfigFile(buildDir, "main", "com.sample")
        assertThat(generated.exists()).isTrue()
        val content = generated.readText()
        assertThat(content).contains("@JsExport")
        assertThat(content).contains("public object BuildKonfig")
    }

    @Test
    fun `non-KMP JVM project is compatible with Configuration Cache`() {
        buildFile.writeText(
            """
            |$jvmBuildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.sample"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'env', 'production'
            |   }
            |}
            """.trimMargin()
        )

        val runner = gradleRunner(projectDir)

        val firstRun = runner
            .withArguments(
                "generateBuildKonfig",
                "--configuration-cache",
                "--configuration-cache-problems=fail",
                "--stacktrace",
            )
            .build()
        assertThat(firstRun.output).contains("Configuration cache entry stored")

        val secondRun = runner
            .withArguments(
                "generateBuildKonfig",
                "--configuration-cache",
                "--configuration-cache-problems=fail",
                "--stacktrace",
            )
            .build()
        assertThat(secondRun.output).contains("Configuration cache entry reused")
    }

    @Test
    fun `non-KMP JS project is compatible with Configuration Cache`() {
        buildFile.writeText(
            """
            |${buildFileHeader("org.jetbrains.kotlin.js")}
            |
            |kotlin {
            |   js {
            |       browser()
            |       nodejs()
            |       binaries.executable()
            |   }
            |}
            |
            |buildkonfig {
            |   packageName = "com.sample"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'env', 'production'
            |   }
            |}
            """.trimMargin()
        )

        // kotlin-js requires a main entry point for the executable.
        val srcDir = projectDir.newFolder("src", "main", "kotlin")
        File(srcDir, "Main.kt").writeText("fun main() {}")

        val runner = gradleRunner(projectDir)

        val firstRun = runner
            .withArguments(
                "generateBuildKonfig",
                "--configuration-cache",
                "--configuration-cache-problems=fail",
                "--stacktrace",
            )
            .build()
        assertThat(firstRun.output).contains("Configuration cache entry stored")

        val secondRun = runner
            .withArguments(
                "generateBuildKonfig",
                "--configuration-cache",
                "--configuration-cache-problems=fail",
                "--stacktrace",
            )
            .build()
        assertThat(secondRun.output).contains("Configuration cache entry reused")
    }

    @Test
    fun `targetConfigs are ignored with a warning on a non-KMP project`() {
        buildFile.writeText(
            """
            |$jvmBuildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.sample"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'env', 'production'
            |   }
            |   targetConfigs {
            |       jvm {
            |           buildConfigField 'STRING', 'env', 'jvmonly'
            |       }
            |   }
            |}
            """.trimMargin()
        )

        val buildDir = projectDir.buildKonfigDir()

        val result = gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        assertThat(result.output)
            .contains("BuildKonfig: targetConfigs are ignored in non-multiplatform projects")

        val generated = buildKonfigFile(buildDir, "main", "com.sample")
        assertThat(generated.exists()).isTrue()
        val content = generated.readText()
        // The defaultConfigs value wins; no expect/actual was generated.
        assertThat(content).contains("public val env: String = \"production\"")
        assertThat(content).doesNotContain("expect ")
        assertThat(content).doesNotContain("actual ")
    }
}
