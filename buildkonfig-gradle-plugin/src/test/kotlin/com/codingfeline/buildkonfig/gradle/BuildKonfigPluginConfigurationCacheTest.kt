package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BuildKonfigPluginConfigurationCacheTest {

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
        |  iosX64()
        |
        |  sourceSets {
        |    commonMain {
        |      dependencies {}
        |    }
        |    jvmMain {
        |      dependencies {}
        |    }
        |  }
        |}
    """.trimMargin()

    @Before
    fun setup() {
        buildFile = projectDir.newFile("build.gradle")
        settingFile = projectDir.newFile("settings.gradle")
        settingFile.writeText(settingsGradle)
    }

    @Test
    fun `generateBuildKonfig is compatible with Configuration Cache`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.sample"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'test', 'hoge'
            |       buildConfigField 'INT', 'intValue', '10'
            |   }
            |
            |   targetConfigs {
            |       jvm {
            |           buildConfigField 'STRING', 'test', 'jvm'
            |       }
            |   }
            |}
            |
            |$buildFileMPPConfig
            """.trimMargin()
        )

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        // First run: store the configuration cache entry.
        val firstRun = runner
            .withArguments(
                "generateBuildKonfig",
                "--configuration-cache",
                "--configuration-cache-problems=fail",
                "--stacktrace"
            )
            .build()

        assertThat(firstRun.output).contains("Configuration cache entry stored")

        // Second run: reuse the configuration cache entry.
        val secondRun = runner
            .withArguments(
                "generateBuildKonfig",
                "--configuration-cache",
                "--configuration-cache-problems=fail",
                "--stacktrace"
            )
            .build()

        assertThat(secondRun.output).contains("Configuration cache entry reused")
    }
}
