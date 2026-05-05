package com.codingfeline.buildkonfig.gradle

import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Common scaffolding for Gradle TestKit-driven tests.
 *
 * Provides a per-test [TemporaryFolder], creates an empty build script, a
 * `settings.gradle` populated with [settingsGradle], and an empty `gradle.properties`
 * subclasses can extend through [extraGradleProperties] and [appendGradleProperties].
 * Additional files (e.g. Kotlin source fixtures) can be written from [extraSetup].
 */
abstract class BaseGradlePluginTest {

    @get:Rule
    val projectDir = TemporaryFolder()

    protected lateinit var buildFile: File

    protected lateinit var settingFile: File

    /** Override to use `build.gradle.kts` instead of the default `build.gradle`. */
    protected open val buildFileName: String = "build.gradle"

    /**
     * Initial `gradle.properties` contents for the fixture. Empty by default. Subclasses
     * can override this when every test in the class needs the same entries; one-off
     * additions can use [appendGradleProperties] from inside the test body.
     */
    protected open val extraGradleProperties: String = ""

    private lateinit var gradleProperties: File

    @Before
    fun baseSetup() {
        buildFile = projectDir.newFile(buildFileName)
        settingFile = projectDir.newFile("settings.gradle")
        settingFile.writeText(settingsGradle)
        gradleProperties = projectDir.newFile("gradle.properties").apply {
            writeText(extraGradleProperties.ensureTrailingNewline())
        }
        extraSetup()
    }

    /**
     * Append additional entries to the per-fixture `gradle.properties`. A trailing
     * newline is inserted automatically so subsequent appends start on their own line.
     */
    protected fun appendGradleProperties(text: String) {
        gradleProperties.appendText(text.ensureTrailingNewline())
    }

    private fun String.ensureTrailingNewline(): String =
        if (isEmpty() || endsWith("\n")) this else this + "\n"

    /**
     * Hook for subclasses to write additional files (e.g. Kotlin source fixtures) after
     * the standard setup has run. For extra `gradle.properties` entries, prefer
     * overriding [extraGradleProperties] instead.
     */
    protected open fun extraSetup() {}
}
