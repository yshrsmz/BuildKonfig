package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class BuildKonfigPluginTest {

    @Test
    fun `Applying plugin with kotlin jvm plugin throws`() {
        val fixtureRoot = File("src/test/kotlin-no-mpp")

        val buildDir = File(fixtureRoot, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(fixtureRoot)
            .withPluginClasspath()

        val result = runner
            .withArguments("build", "--stacktrace")
            .buildAndFail()

        assertThat(result.output)
            .contains("BuildKonfig Gradle plugin applied in project ':' but no supported Kotlin multiplatform plugin was found")
    }

    @Test
    fun `buildkonfig block without defaultConfigs throws`() {
        val fixtureRoot = File("src/test/kotlin-mpp-no-defaultconfigs")

        val buildDir = File(fixtureRoot, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(fixtureRoot)
            .withPluginClasspath()

        val result = runner
            .withArguments("build", "--stacktrace")
            .buildAndFail()

        assertThat(result.output)
            .contains("non flavored defaultConfigs must be provided")
    }


    @Test
    fun `buildkonfig block without default targetConfigs works fine`() {
        val fixtureRoot = File("src/test/kotlin-mpp-no-default-targetconfigs")

        val buildDir = File(fixtureRoot, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(fixtureRoot)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()

        assertThat(result.output)
            .contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `Applying the plugin works fine for multiplatform project`() {
        val fixtureRoot = File("src/test/kotlin-mpp")

        val buildDir = File(fixtureRoot, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(fixtureRoot)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace")
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
            .contains("generateBuildKonfig")
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
            .contains("generateBuildKonfig")

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
            .contains("generateBuildKonfig")
    }
}
