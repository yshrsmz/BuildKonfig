package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test

class BuildKonfigPluginIsolatedProjectsTest : BaseGradlePluginTest() {

    @Test
    fun `buildkonfig configures in a subproject under Isolated Projects`() {
        settingFile.appendText("\ninclude ':config'\n")

        projectDir.newFolder("config")
        projectDir.newFile("config/build.gradle").writeText(
            """
            |${buildFileHeader("kotlin-multiplatform")}
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'value', 'x'
            |   }
            |}
            |
            |kotlin {
            |  jvm()
            |}
            """.trimMargin()
        )

        // Pinned to a Gradle version where the parent-project property lookup is a hard Isolated
        // Projects violation. Gradle 9.6.0+ no longer flags it, so a floating version would not guard
        // the regression.
        val result = gradleRunner(projectDir)
            .withGradleVersion("9.4.1")
            .withArguments(
                ":config:generateBuildKonfig",
                "-Dorg.gradle.unsafe.isolated-projects=true",
                "--stacktrace",
            )
            .build()
            .assertBuildSuccessful()

        assertThat(result.task(":config:generateBuildKonfig")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
}
