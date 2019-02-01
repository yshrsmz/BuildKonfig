package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class BuildKonfigPluginTest {

    @Test
    fun `Applying the plugin works fine for multiplatform project`() {
        val fixtureRoot = File("src/test/kotlin-mpp")

        val buildDir = File(fixtureRoot, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(fixtureRoot)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateMainBuildKonfig", "--stacktrace")
            .build()

        assertThat(result.output)
            .contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `The generate task is a dependency of multiplatform jvm target`() {
        val fixtureRoot = File("src/test/kotlin-mpp")

        val buildDir = File(fixtureRoot, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(fixtureRoot)
            .withPluginClasspath()

        val result = runner
            .withArguments("compileKotlinJvm", "--stacktrace")
            .build()

        assertThat(result.output)
            .contains("generateMainBuildKonfig")

        assertThat(result.output)
            .doesNotContain("generateTestBuildKonfig")
    }

    @Test
    fun `The generate task is a dependency of multiplatform jvm test target`() {
        val fixtureRoot = File("src/test/kotlin-mpp")

        val buildDir = File(fixtureRoot, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(fixtureRoot)
            .withPluginClasspath()

        val result = runner
            .withArguments("compileTestKotlinJvm", "--stacktrace")
            .build()

        assertThat(result.output)
            .contains("generateTestBuildKonfig")

//        assertThat(result.output)
//            .doesNotContain("generateMainBuildKonfig")
    }

    @Test
    fun `The generate task is a dependency of multiplatform js target`() {
        val fixtureRoot = File("src/test/kotlin-mpp")

        val buildDir = File(fixtureRoot, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(fixtureRoot)
            .withPluginClasspath()

        val result = runner
            .withArguments("compileKotlinJs", "--stacktrace")
            .build()

        assertThat(result.output)
            .contains("generateMainBuildKonfig")

        assertThat(result.output)
            .doesNotContain("generateTestBuildKonfig")
    }
}
