package com.codingfeline.buildkonfig.gradle

import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Common scaffolding for Gradle TestKit-driven tests.
 *
 * Provides a per-test [TemporaryFolder], creates an empty build script and a
 * `settings.gradle` populated with [settingsGradle], and exposes [extraSetup] for
 * subclass-specific files such as `gradle.properties` or source fixtures.
 */
abstract class BaseGradlePluginTest {

    @get:Rule
    val projectDir = TemporaryFolder()

    protected lateinit var buildFile: File

    protected lateinit var settingFile: File

    /** Override to use `build.gradle.kts` instead of the default `build.gradle`. */
    protected open val buildFileName: String = "build.gradle"

    @Before
    fun baseSetup() {
        buildFile = projectDir.newFile(buildFileName)
        settingFile = projectDir.newFile("settings.gradle")
        settingFile.writeText(settingsGradle)
        extraSetup()
    }

    /**
     * Hook for subclasses to write additional files (e.g. `gradle.properties`,
     * Kotlin source fixtures) after the standard setup has run.
     */
    protected open fun extraSetup() {}
}
