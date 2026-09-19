package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.gradle.util.GradleVersion
import org.junit.Test

/**
 * The failure path cannot be exercised through TestKit: running an older Gradle needs an
 * older JDK than the one the suite runs on, so the check is unit tested directly.
 */
class GradleVersionCheckTest {

    @Test
    fun `accepts the minimum supported version`() {
        checkGradleVersion(GradleVersion.version("8.14"))
    }

    @Test
    fun `accepts newer versions`() {
        checkGradleVersion(GradleVersion.version("9.7.1"))
    }

    @Test
    fun `rejects older versions`() {
        val failure = runCatching { checkGradleVersion(GradleVersion.version("8.13")) }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        assertThat(failure).hasMessageThat().contains("requires Gradle 8.14 or later")
        assertThat(failure).hasMessageThat().contains("8.13")
    }
}
