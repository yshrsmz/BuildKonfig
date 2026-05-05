package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BuildKonfigPluginConfigurationCacheTest : BaseGradlePluginTest() {

    private val buildFileHeader = buildFileHeader("kotlin-multiplatform")

    private val buildFileKMPConfig = """
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
            |$buildFileKMPConfig
            """.trimMargin()
        )

        val runner = gradleRunner(projectDir)

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
