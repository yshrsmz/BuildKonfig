plugins {
    // Use `apply false` to avoid plugins to be loaded multiple times in each subproject's
    // classloader. See also, https://youtrack.jetbrains.com/issue/KT-46200

    @Suppress("DSL_SCOPE_VIOLATION") // See also, https://github.com/gradle/gradle/issues/22797#issuecomment-1517046458
    run {
        alias(libs.plugins.kotlin.multiplatform) apply false
        alias(libs.plugins.kotlin.android) apply false
        alias(libs.plugins.kotlin.jvm) apply false

        alias(libs.plugins.android.library) apply false
    }

    id("com.codingfeline.buildkonfig") version "+" apply false
}
