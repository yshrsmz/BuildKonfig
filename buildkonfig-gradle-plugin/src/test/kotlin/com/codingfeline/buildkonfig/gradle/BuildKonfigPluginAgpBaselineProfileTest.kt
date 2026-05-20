package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test

/**
 * Regression test for issue #320: AGP 9.0+ `prepareAndroidMainArtProfile`
 * (`ProcessLibraryArtProfileTask`) probes `<generatedSourceRoot>/baselineProfiles/baseline-prof.txt`
 * for every generated source directory it inherits from the Kotlin source set.
 *
 * The task's `@OutputDirectory` must therefore be scoped tightly enough that
 * `<root>/baselineProfiles/...` is not considered an output of `generateBuildKonfig`,
 * otherwise Gradle 9.x rejects the build with:
 *
 *     Task ':...:prepareAndroidMainArtProfile' uses this output of task
 *     ':...:generateBuildKonfig' without declaring an explicit or implicit dependency.
 */
class BuildKonfigPluginAgpBaselineProfileTest : BaseGradlePluginTest() {

    private val androidBuildFileHeader =
        buildFileHeader("kotlin-multiplatform", "com.android.kotlin.multiplatform.library")

    @Test
    fun `prepareAndroidMainArtProfile does not fail with implicit dependency error`() {
        buildFile.writeText(
            """
            |$androidBuildFileHeader
            |
            |buildkonfig {
            |    packageName = "com.sample"
            |
            |    defaultConfigs {
            |        buildConfigField 'STRING', 'test', 'hoge'
            |    }
            |}
            |
            |kotlin {
            |   android {
            |       compileSdk = 28
            |       minSdk = 21
            |       namespace = "com.sample"
            |   }
            |   jvm()
            |   iosX64()
            |
            |   sourceSets {
            |     commonMain {
            |       dependencies {}
            |     }
            |     androidMain {
            |       dependencies {}
            |     }
            |   }
            |}
            """.trimMargin()
        )

        createAndroidManifest(projectDir)

        // Force `compileAndroidMain` to have something to compile, exercising the
        // generateBuildKonfig → compileAndroidMain dependency edge through the
        // androidMain Kotlin source set's srcDirs.
        projectDir.newFolder("src", "androidMain", "kotlin")
        projectDir.newFile("src/androidMain/kotlin/Sample.kt").writeText(
            """
            |package com.sample
            |
            |object Sample
            """.trimMargin()
        )

        projectDir.buildKonfigDir()

        val result = gradleRunner(projectDir)
            // Pin to a Gradle 9.x release so the strict input/output overlap validation
            // (the rule whose absence this test is guarding against) is always exercised
            // regardless of the runtime Gradle TestKit happens to default to. AGP 9.2.x
            // — the version used by this fixture's `com.android.kotlin.multiplatform.library`
            // plugin — requires Gradle 9.4.1 or newer, so we pin to 9.4.1.
            .withGradleVersion("9.4.1")
            .withArguments("assembleAndroidMain", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        // The exact validation error message from Gradle 9.x must not surface.
        assertThat(result.output)
            .doesNotContain("without declaring an explicit or implicit dependency")
        // generateBuildKonfig must have actually been included in the task graph
        // (i.e. its srcDir is correctly wired into the androidMain Kotlin source set).
        val outcome = result.task(":generateBuildKonfig")?.outcome
        assertThat(outcome).isAnyOf(
            TaskOutcome.SUCCESS,
            TaskOutcome.FROM_CACHE,
            TaskOutcome.UP_TO_DATE,
        )
    }
}
