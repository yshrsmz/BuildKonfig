package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BuildKonfigPluginConstFieldsTest : BaseGradlePluginTest() {

    override val buildFileName: String = "build.gradle.kts"

    private val buildFileHeader = buildFileHeaderKts("kotlin-multiplatform")

    private val buildFileKMPConfig = """
        |kotlin {
        |  jvm()
        |  js(IR) {
        |    browser()
        |    nodejs()
        |  }
        |}
    """.trimMargin()

    @Test
    fun `const values produce expect val on common and actual const val on targets`() {
        buildFile.writeText(
            """
            |import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigConstField(Type.STRING, "foo", "defaultValue")
            |       buildConfigField(type = Type.STRING, name = "bar", value = "defaultBarValue", const = true)
            |   }
            |   targetConfigs {
            |       create("js") {
            |           buildConfigConstField(Type.STRING, "foo", "jsValue")
            |           buildConfigField(type = Type.STRING, name = "bar", value = "jsBarValue", const = true)
            |       }
            |   }
            |}
            |
            |$buildFileKMPConfig
            """.trimMargin()
        )

        val buildDir = projectDir.buildKonfigDir()

        val result = gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace", "--warning-mode=all")
            .build()
            .assertBuildSuccessful()

        assertThat(result.output).apply {
            contains("declared with `const = true` but target-specific configs are present")
            contains("foo")
            contains("bar")
        }

        val commonResult = buildKonfigFile(buildDir, "commonMain", "com.example")
        assertThat(commonResult.readText()).apply {
            contains("public val foo: String")
            contains("public val bar: String")
            doesNotContain("const val foo")
            doesNotContain("const val bar")
            doesNotContain("CONST_VAL_WITHOUT_INITIALIZER")
        }

        val jvmResult = buildKonfigFile(buildDir, "jvmMain", "com.example")
        assertThat(jvmResult.readText()).apply {
            contains("actual const val foo: String = \"defaultValue\"")
            contains("actual const val bar: String = \"defaultBarValue\"")
        }

        val jsResult = buildKonfigFile(buildDir, "jsMain", "com.example")
        assertThat(jsResult.readText()).apply {
            contains("actual const val foo: String = \"jsValue\"")
            contains("actual const val bar: String = \"jsBarValue\"")
        }
    }
}
