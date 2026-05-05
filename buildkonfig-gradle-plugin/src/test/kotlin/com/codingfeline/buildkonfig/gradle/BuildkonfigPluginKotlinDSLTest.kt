package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BuildkonfigPluginKotlinDSLTest : BaseGradlePluginTest() {

    override val buildFileName: String = "build.gradle.kts"

    private val androidBuildFileHeader = buildFileHeaderKts("kotlin-multiplatform", "com.android.library")

    @Test
    fun `issue 50 - js(IR) target`() {
        buildFile.writeText(
            """
            |import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
            |import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
            |
            |$androidBuildFileHeader
            |
            |kotlin {
            |    androidTarget()
            |
            |    iosX64()
            |    iosArm64()
            |    iosSimulatorArm64()
            |
            |    js(IR) {
            |        browser()
            |    }
            |
            |    applyDefaultHierarchyTemplate()
            |
            |    sourceSets {
            |        val commonMain by getting
            |
            |        val androidMain by getting
            |
            |        val iosMain by getting
            |
            |        val jsMain by getting
            |    }
            |}
            |
            |configure<com.codingfeline.buildkonfig.gradle.BuildKonfigExtension> {
            |    packageName = "com.sample.buildkonfig.issues"
            |
            |    defaultConfigs {
            |        buildConfigField(Type.STRING, "VERSION", "mysupersecretpassword")
            |    }
            |}
            |
            |android {
            |    compileSdkVersion(30)
            |    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
            |    defaultConfig {
            |        minSdkVersion(21)
            |        targetSdkVersion(30)
            |    }
            |
            |    namespace = "com.sample"
            |}
            """.trimMargin()
        )

        createAndroidManifest(projectDir)

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        val commonResult = buildKonfigFile(buildDir, "commonMain", "com.sample.buildkonfig.issues")
        assertThat(commonResult.exists()).isTrue()

        val androidResult = buildKonfigFile(buildDir, "androidMain", "com.sample.buildkonfig.issues")
        assertThat(androidResult.exists()).isFalse()

        val jsResult = buildKonfigFile(buildDir, "jsMain", "com.sample.buildkonfig.issues")
        assertThat(jsResult.exists()).isFalse()
    }
}
