package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth
import org.junit.Test

class BuildKonfigPluginKotlinDSLFlavorTest : BaseGradlePluginTest() {

    override val buildFileName: String = "build.gradle.kts"

    private val buildFileHeader = buildFileHeaderKts("kotlin-multiplatform")

    private val buildFileKMPConfig = """
        |kotlin {
        |  jvm()
        |  js(IR) {
        |    browser()
        |    nodejs()
        |  }
        |  iosX64()
        |  iosArm64()
        |  iosSimulatorArm64()
        |}
    """.trimMargin()

    @Test
    fun `Flavored targetConfigs overwrites default targetConfigs`() {
        buildFile.writeText(
            """
            |import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField(Type.STRING, "value", "defaultValue")
            |   }
            |   targetConfigs {
            |       create("js") {
            |           buildConfigField(Type.STRING,"value", "foobar")
            |           buildConfigField(type = Type.STRING, name = "overwrittenValue", value = "defaultJsValue")
            |       }
            |   }
            |   targetConfigs("dev") {
            |       create("js") {
            |           buildConfigField(type = Type.STRING, name = "overwrittenValue", value = "devJsValue")
            |       }
            |   }
            |}
            |
            |$buildFileKMPConfig
            """.trimMargin()
        )

        val propertyFile = projectDir.newFile("gradle.properties")
        propertyFile.writeText("buildkonfig.flavor=dev")

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        val jvmResult = buildKonfigFile(buildDir, "jvmMain", "com.example")
        Truth.assertThat(jvmResult.readText())
            .contains("defaultValue")

        val jsResult = buildKonfigFile(buildDir, "jsMain", "com.example")
        Truth.assertThat(jsResult.readText())
            .apply {
                contains("foobar")
                contains("devJsValue")
            }

        val iosResult = buildKonfigFile(buildDir, "iosX64Main", "com.example")
        Truth.assertThat(iosResult.readText())
            .contains("defaultValue")
    }
}
