package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Regression test for issue #236: Gradle 9.0 + KSP combinations failed with an "implicit
 * dependency" error because KSP tasks consume the `generateBuildKonfig` output directory
 * via `kotlin.srcDirs(...)` without a declared task dependency. After the lazy/Provider
 * refactor the source set Provider chain establishes the dependency automatically.
 */
class BuildKonfigPluginGradle9KspTest {

    @get:Rule
    val projectDir = TemporaryFolder()

    lateinit var buildFile: File

    lateinit var settingFile: File

    private val buildFileHeader = buildFileHeader("kotlin-multiplatform", "com.google.devtools.ksp")

    @Before
    fun setup() {
        buildFile = projectDir.newFile("build.gradle")
        settingFile = projectDir.newFile("settings.gradle")
        settingFile.writeText(settingsGradle)
    }

    @Test
    fun `KSP task does not fail with implicit dependency error on Gradle 9 x`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.sample"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'test', 'hoge'
            |   }
            |}
            |
            |kotlin {
            |  jvm()
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
        )

        // Provide a dummy Kotlin source so that compileKotlinJvm has something to compile,
        // exercising the source set wiring (and therefore the Provider-based task dependency
        // edge from the source set into generateBuildKonfig).
        projectDir.newFolder("src", "jvmMain", "kotlin")
        projectDir.newFile("src/jvmMain/kotlin/Sample.kt").writeText(
            """
            |package com.sample
            |
            |object Sample
            """.trimMargin()
        )

        val runner = GradleRunner.create()
            .withGradleVersion("9.3.1")
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("compileKotlinJvm", "--stacktrace")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        // Implicit-dependency error message from Gradle 9.x must not surface.
        assertThat(result.output)
            .doesNotContain("without declaring an explicit or implicit dependency")
        // generateBuildKonfig must have actually run.
        assertThat(result.output).contains("generateBuildKonfig")
    }
}
