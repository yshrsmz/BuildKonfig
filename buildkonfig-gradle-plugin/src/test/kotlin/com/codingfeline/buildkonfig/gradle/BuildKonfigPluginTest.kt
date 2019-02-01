package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class BuildKonfigPluginTest {

    @Test
    fun `read configs`() {
        val fixtureRoot = File("src/test/kotlin-mpp")

        val buildDir = File(fixtureRoot, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(fixtureRoot)
            .withPluginClasspath()

        val result = runner.withArguments("generateMainBuildKonfig", "--stacktrace", "--info")
            .buildAndFail()

        Truth.assertThat(result.output)
            .contains("Kgql Gradle Plugin applied in project ':' but no supported Kotlin plugin was found")
    }
}
