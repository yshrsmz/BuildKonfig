package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
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

    private val configurationCacheArgs = listOf(
        "generateBuildKonfig",
        "--configuration-cache",
        "--configuration-cache-problems=fail",
        "--stacktrace",
    )

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
            .withArguments(configurationCacheArgs)
            .build()

        assertThat(firstRun.output).contains("Configuration cache entry stored")

        // Second run: reuse the configuration cache entry.
        val secondRun = runner
            .withArguments(configurationCacheArgs)
            .build()

        assertThat(secondRun.output).contains("Configuration cache entry reused")
    }

    /**
     * The task dependency edge from a downstream Kotlin compile task back to
     * `generateBuildKonfig` flows through the `MapProperty.getting(key)` Provider chain
     * registered on the Kotlin source set. This test exercises that chain end-to-end
     * under `--configuration-cache` to catch any regression in CC serialization of
     * `@OutputDirectories MapProperty<String, Directory>`.
     */
    @Test
    fun `compileKotlinJvm exercises the source set Provider chain under Configuration Cache`() {
        // `targetConfigs.jvm` is required so `decideOutputs()` registers a `jvmMain`
        // entry in `outputDirectories` in addition to `commonMain`. Without it the map
        // is single-entry and `compileKotlinJvm` would only reach `generateBuildKonfig`
        // through the commonMain srcDir wiring — the per-leaf jvmMain Provider chain
        // (`task.flatMap { it.outputDirectories.getting("jvmMain") }`) we want to guard
        // against CC regressions would not actually be walked.
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
            |
            |   targetConfigs {
            |       jvm {
            |           buildConfigField 'STRING', 'jvmOnly', 'x'
            |       }
            |   }
            |}
            |
            |$buildFileKMPConfig
            """.trimMargin()
        )

        // A dummy source so that compileKotlinJvm has something to compile, forcing the
        // Provider chain from jvmMain.kotlin.srcDirs through to generateBuildKonfig to
        // be walked when Gradle builds the task graph.
        projectDir.newFolder("src", "jvmMain", "kotlin")
        projectDir.newFile("src/jvmMain/kotlin/Sample.kt").writeText(
            """
            |package com.sample
            |
            |object Sample
            """.trimMargin()
        )

        val runner = gradleRunner(projectDir)
            .withGradleVersion("9.3.1")

        val firstRun = runner
            .withArguments(
                "compileKotlinJvm",
                "--configuration-cache",
                "--configuration-cache-problems=fail",
                "--stacktrace",
            )
            .build()
            .assertBuildSuccessful()

        assertThat(firstRun.output).contains("Configuration cache entry stored")
        // generateBuildKonfig must be picked up as a transitive dependency of compileKotlinJvm.
        assertThat(firstRun.task(":generateBuildKonfig")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        val secondRun = runner
            .withArguments(
                "compileKotlinJvm",
                "--configuration-cache",
                "--configuration-cache-problems=fail",
                "--stacktrace",
            )
            .build()
            .assertBuildSuccessful()

        assertThat(secondRun.output).contains("Configuration cache entry reused")
        // On the second run the inputs are unchanged, so the task should be up-to-date.
        assertThat(secondRun.task(":generateBuildKonfig")?.outcome).isAnyOf(
            TaskOutcome.UP_TO_DATE,
            TaskOutcome.FROM_CACHE,
        )
    }

    private fun writeFlavorPropertiesBuild() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |   flavorPropertiesFile.set(layout.projectDirectory.file('flavor.properties'))
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'stringValue', 'defaultValue'
            |   }
            |   defaultConfigs("dev") {
            |       buildConfigField 'STRING', 'stringValue', 'devDefaultValue'
            |   }
            |   defaultConfigs("release") {
            |       buildConfigField 'STRING', 'stringValue', 'releaseDefaultValue'
            |   }
            |}
            |
            |$buildFileKMPConfig
            """.trimMargin()
        )
    }

    /**
     * Stores and reuses a configuration cache entry, applies [changeFlavorFile], and asserts
     * that the next run discards the entry and generates [expectedAfter].
     */
    private fun assertFlavorFileChangeInvalidatesCache(
        expectedBefore: String,
        changeFlavorFile: () -> Unit,
        expectedAfter: String,
    ) {
        val buildDir = projectDir.buildKonfigDir()
        val runner = gradleRunner(projectDir).withArguments(configurationCacheArgs)

        val firstRun = runner.build().assertBuildSuccessful()
        assertThat(firstRun.output).contains("Configuration cache entry stored")
        assertThat(buildKonfigFile(buildDir, "commonMain", "com.example").readText())
            .contains("val stringValue: String = \"$expectedBefore\"")

        val secondRun = runner.build().assertBuildSuccessful()
        assertThat(secondRun.output).contains("Configuration cache entry reused")

        changeFlavorFile()

        val thirdRun = runner.build().assertBuildSuccessful()
        assertThat(thirdRun.output).contains("cannot be reused")
        assertThat(thirdRun.output).doesNotContain("Configuration cache entry reused")
        assertThat(buildKonfigFile(buildDir, "commonMain", "com.example").readText())
            .contains("val stringValue: String = \"$expectedAfter\"")
    }

    @Test
    fun `editing the flavor properties file invalidates the configuration cache`() {
        writeFlavorPropertiesBuild()
        val flavorFile = projectDir.newFile("flavor.properties")
        flavorFile.writeText("buildkonfig.flavor=dev")

        assertFlavorFileChangeInvalidatesCache(
            expectedBefore = "devDefaultValue",
            changeFlavorFile = { flavorFile.writeText("buildkonfig.flavor=release") },
            expectedAfter = "releaseDefaultValue",
        )
    }

    @Test
    fun `creating a missing flavor properties file invalidates the configuration cache`() {
        writeFlavorPropertiesBuild()

        assertFlavorFileChangeInvalidatesCache(
            expectedBefore = "defaultValue",
            changeFlavorFile = { projectDir.newFile("flavor.properties").writeText("buildkonfig.flavor=dev") },
            expectedAfter = "devDefaultValue",
        )
    }

    @Test
    fun `deleting the flavor properties file invalidates the configuration cache`() {
        writeFlavorPropertiesBuild()
        val flavorFile = projectDir.newFile("flavor.properties")
        flavorFile.writeText("buildkonfig.flavor=dev")

        assertFlavorFileChangeInvalidatesCache(
            expectedBefore = "devDefaultValue",
            changeFlavorFile = { check(flavorFile.delete()) },
            expectedAfter = "defaultValue",
        )
    }
}
