package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BuildKonfigPluginFlavorTest : BaseGradlePluginTest() {

    private val buildFileHeader = buildFileHeader("kotlin-multiplatform")

    private val buildFileKMPConfig = """
        |kotlin {
        |  jvm()
        |  js(IR) {
        |    browser()
        |    nodejs()
        |  }
        |  iosX64()
        |}
    """.trimMargin()

    private val buildFileKMPConfigWithWasmJs = """
        |kotlin {
        |  jvm()
        |  js(IR) {
        |    browser()
        |    nodejs()
        |  }
        |  wasmJs {
        |    browser()
        |  }
        |  iosX64()
        |}
    """.trimMargin()

    private val buildFileKMPConfigWasmJsOnly = """
        |kotlin {
        |  jvm()
        |  wasmJs {
        |    browser()
        |  }
        |  iosX64()
        |}
    """.trimMargin()

    private fun writeFlavorDevProperties() {
        appendGradleProperties("buildkonfig.flavor=dev")
    }

    @Test
    fun `common object should be generated if there is no targetConfigs`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'stringValue', 'defaultValue'
            |   }
            |}
            |$buildFileKMPConfig
        """.trimMargin()
        )

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        val commonResult = buildKonfigFile(buildDir, "commonMain", "com.example")
        assertThat(commonResult.readText())
            .isEqualTo(
                """
                |package com.example
                |
                |import kotlin.String
                |
                |internal object BuildKonfig {
                |  public val stringValue: String = "defaultValue"
                |}
                |
            """.trimMargin()
            )

        assertThat(buildKonfigFile(buildDir, "jvmMain", "com.example").exists()).isFalse()
        assertThat(buildKonfigFile(buildDir, "jsMain", "com.example").exists()).isFalse()
        assertThat(buildKonfigFile(buildDir, "iosX64Main", "com.example").exists()).isFalse()
    }

    @Test
    fun `flavor can be obtained from gradle properties file`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'stringValue', 'defaultValue'
            |   }
            |   defaultConfigs("dev") {
            |       buildConfigField 'STRING', 'stringValue', 'devDefaultValue'
            |   }
            |}
            |$buildFileKMPConfig
        """.trimMargin()
        )

        writeFlavorDevProperties()

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        val commonResult = buildKonfigFile(buildDir, "commonMain", "com.example")
        assertThat(commonResult.readText())
            .contains("val stringValue: String = \"devDefaultValue\"")

        assertThat(buildKonfigFile(buildDir, "jvmMain", "com.example").exists()).isFalse()
        assertThat(buildKonfigFile(buildDir, "jsMain", "com.example").exists()).isFalse()
        assertThat(buildKonfigFile(buildDir, "iosX64Main", "com.example").exists()).isFalse()
    }

    @Test
    fun `flavor can be overwritten by cli parameter`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'stringValue', 'defaultValue'
            |   }
            |   defaultConfigs("dev") {
            |       buildConfigField 'STRING', 'stringValue', 'devDefaultValue'
            |   }
            |   defaultConfigs("release") {
            |       buildConfigField 'STRING', 'stringValue', 'releaseDefaultValue'
            |   }
            |}
            |$buildFileKMPConfig
        """.trimMargin()
        )

        writeFlavorDevProperties()

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace", "-Pbuildkonfig.flavor=release")
            .build()
            .assertBuildSuccessful()

        val commonResult = buildKonfigFile(buildDir, "commonMain", "com.example")
        assertThat(commonResult.readText())
            .contains("val stringValue: String = \"releaseDefaultValue\"")

        assertThat(buildKonfigFile(buildDir, "jvmMain", "com.example").exists()).isFalse()
        assertThat(buildKonfigFile(buildDir, "jsMain", "com.example").exists()).isFalse()
        assertThat(buildKonfigFile(buildDir, "iosX64Main", "com.example").exists()).isFalse()
    }

    @Test
    fun `Default targetConfigs overwrite flavored defaultConfigs`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'stringValue', 'defaultValue'
            |   }
            |   defaultConfigs("dev") {
            |       buildConfigField 'STRING', 'stringValue', 'devDefaultValue'
            |   }
            |   targetConfigs {
            |       jvm {
            |           buildConfigField 'STRING', 'stringValue', 'jvmDefaultValue'
            |       }
            |   }
            |}
            |$buildFileKMPConfig
        """.trimMargin()
        )

        writeFlavorDevProperties()

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        assertThat(buildKonfigFile(buildDir, "jvmMain", "com.example").readText())
            .contains("jvmDefaultValue")
        assertThat(buildKonfigFile(buildDir, "jsMain", "com.example").readText())
            .contains("devDefaultValue")
        assertThat(buildKonfigFile(buildDir, "iosX64Main", "com.example").readText())
            .contains("devDefaultValue")
    }

    @Test
    fun `Flavored targetConfigs overwrite flavored defaultConfigs`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'stringValue', 'defaultValue'
            |   }
            |   defaultConfigs("dev") {
            |       buildConfigField 'STRING', 'stringValue', 'devDefaultValue'
            |   }
            |   targetConfigs("dev") {
            |       js {
            |           buildConfigField 'STRING', 'stringValue', 'devJsValue'
            |       }
            |   }
            |}
            |$buildFileKMPConfig
        """.trimMargin()
        )

        writeFlavorDevProperties()

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        assertThat(buildKonfigFile(buildDir, "jvmMain", "com.example").readText())
            .contains("devDefaultValue")
        assertThat(buildKonfigFile(buildDir, "jsMain", "com.example").readText())
            .contains("devJsValue")
        assertThat(buildKonfigFile(buildDir, "iosX64Main", "com.example").readText())
            .contains("devDefaultValue")
    }

    @Test
    fun `Can create nullable field`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigNullableField 'STRING', 'stringValue', 'defaultValue'
            |       buildConfigField 'INT', 'intValue', '10', nullable: true
            |   }
            |}
            |$buildFileKMPConfig
        """.trimMargin()
        )

        writeFlavorDevProperties()

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        val commonResult = buildKonfigFile(buildDir, "commonMain", "com.example")
        assertThat(commonResult.readText()).apply {
            contains("stringValue: String? = \"defaultValue\"")
            contains("intValue: Int? = 10")
        }
    }

    @Test
    fun `Can assign null to nullable field`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'stringValue', null, nullable: true
            |       buildConfigNullableField 'INT', 'intValue', null
            |   }
            |}
            |$buildFileKMPConfig
        """.trimMargin()
        )

        writeFlavorDevProperties()

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        val commonResult = buildKonfigFile(buildDir, "commonMain", "com.example")
        assertThat(commonResult.readText()).apply {
            contains("stringValue: String? = null")
            contains("intValue: Int? = null")
        }
    }

    @Test
    fun `field replacement during flavored target config merge is logged`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'unrelated', 'unused'
            |   }
            |   targetConfigs {
            |       jvm {
            |           buildConfigField 'STRING', 'key', 'defaultJvm'
            |       }
            |   }
            |   targetConfigs("dev") {
            |       jvm {
            |           buildConfigField 'STRING', 'key', 'devJvm'
            |       }
            |   }
            |}
            |$buildFileKMPConfig
        """.trimMargin()
        )

        writeFlavorDevProperties()

        val result = gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace", "--info")
            .build()
            .assertBuildSuccessful()

        // Pins the merge-replacement log line: future refactors of mergeConfigs /
        // mergeTargetConfigs should preserve it (or update this assertion deliberately).
        assertThat(result.output)
            .contains("BuildKonfig(jvmMain): field is being replaced with flavored(dev): defaultJvm -> devJvm")
    }

    @Test
    fun `Flavored targetConfigs overwrite default targetConfigs`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'key', 'defaultValue'
            |   }
            |   targetConfigs {
            |       js {
            |           buildConfigField 'STRING', 'key', 'defaultJsValue'
            |       }
            |   }
            |   targetConfigs("dev") {
            |       js {
            |           buildConfigField 'STRING', 'key', 'devJsValue'
            |       }
            |   }
            |}
            |$buildFileKMPConfig
        """.trimMargin()
        )

        writeFlavorDevProperties()

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        assertThat(buildKonfigFile(buildDir, "jvmMain", "com.example").readText())
            .contains("defaultValue")
        assertThat(buildKonfigFile(buildDir, "jsMain", "com.example").readText())
            .contains("devJsValue")
        assertThat(buildKonfigFile(buildDir, "iosX64Main", "com.example").readText())
            .contains("defaultValue")
    }

    @Test
    fun `Passing non-existent flavor results in default configurations`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'stringValue', 'defaultValue'
            |   }
            |   targetConfigs {
            |       js {
            |           buildConfigField 'STRING', 'stringValue', 'defaultJsValue'
            |       }
            |   }
            |   targetConfigs("dev") {
            |       js {
            |           buildConfigField 'STRING', 'stringValue', 'devJsValue'
            |       }
            |   }
            |}
            |$buildFileKMPConfig
        """.trimMargin()
        )

        writeFlavorDevProperties()

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace", "-Pbuildkonfig.flavor=nonexistent")
            .build()
            .assertBuildSuccessful()

        assertThat(buildKonfigFile(buildDir, "jvmMain", "com.example").readText())
            .contains("defaultValue")
        assertThat(buildKonfigFile(buildDir, "jsMain", "com.example").readText())
            .contains("defaultJsValue")
        assertThat(buildKonfigFile(buildDir, "iosX64Main", "com.example").readText())
            .contains("defaultValue")
    }

    @Test
    fun `The generated object uses the given objectName`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |   objectName = "AwesomeConfig"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'stringValue', 'defaultValue'
            |   }
            |}
            |$buildFileKMPConfig
        """.trimMargin()
        )

        writeFlavorDevProperties()

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        val commonResult = buildKonfigFile(buildDir, "commonMain", "com.example", objectName = "AwesomeConfig")
        assertThat(commonResult.readText()).apply {
            contains("internal object AwesomeConfig")
        }
    }

    @Test
    fun `The generated target objects use the given objectName`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |   objectName = "AwesomeConfig"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'stringValue', 'defaultValue'
            |   }
            |   targetConfigs {
            |       js {
            |         buildConfigField 'STRING', 'stringValue', 'jsValue'
            |       }
            |   }
            |}
            |$buildFileKMPConfig
        """.trimMargin()
        )

        writeFlavorDevProperties()

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        val commonResult = buildKonfigFile(buildDir, "commonMain", "com.example", objectName = "AwesomeConfig")
        assertThat(commonResult.readText()).apply {
            contains("internal expect object AwesomeConfig")
        }

        assertThat(buildKonfigFile(buildDir, "jsMain", "com.example", objectName = "AwesomeConfig").readText())
            .contains("internal actual object AwesomeConfig")
        assertThat(buildKonfigFile(buildDir, "jvmMain", "com.example", objectName = "AwesomeConfig").readText())
            .contains("internal actual object AwesomeConfig")
        assertThat(buildKonfigFile(buildDir, "iosX64Main", "com.example", objectName = "AwesomeConfig").readText())
            .contains("internal actual object AwesomeConfig")
    }

    @Test
    fun `The generated object uses the given objectName and is public`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |   exposeObjectWithName = "AwesomeConfig"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'stringValue', 'defaultValue'
            |   }
            |}
            |$buildFileKMPConfig
        """.trimMargin()
        )

        writeFlavorDevProperties()

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        val commonResult = buildKonfigFile(buildDir, "commonMain", "com.example", objectName = "AwesomeConfig")
        assertThat(commonResult.readText()).apply {
            contains("@JsExport")
            contains("@OptIn(ExperimentalJsExport::class)")
            contains("object AwesomeConfig")
        }
    }

    @Test
    fun `The generated target objects use the given objectName and are public`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |   exposeObjectWithName = "AwesomeConfig"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'stringValue', 'defaultValue'
            |   }
            |   targetConfigs {
            |       js {
            |         buildConfigField 'STRING', 'stringValue', 'jsValue'
            |       }
            |   }
            |}
            |$buildFileKMPConfig
        """.trimMargin()
        )

        writeFlavorDevProperties()

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        val commonResult = buildKonfigFile(buildDir, "commonMain", "com.example", objectName = "AwesomeConfig")
        assertThat(commonResult.readText()).apply {
            contains("object AwesomeConfig")
        }

        val jsResult = buildKonfigFile(buildDir, "jsMain", "com.example", objectName = "AwesomeConfig")
        assertThat(jsResult.readText()).apply {
            contains("@JsExport")
            contains("@OptIn(ExperimentalJsExport::class)")
            contains("object AwesomeConfig")
        }

        assertThat(buildKonfigFile(buildDir, "jvmMain", "com.example", objectName = "AwesomeConfig").readText())
            .contains("object AwesomeConfig")
        assertThat(buildKonfigFile(buildDir, "iosX64Main", "com.example", objectName = "AwesomeConfig").readText())
            .contains("object AwesomeConfig")
    }

    @Test
    fun `When both js and wasmJs targets exist, expect actual is forced and JsExport is only on js actual`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |   exposeObjectWithName = "AwesomeConfig"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'stringValue', 'defaultValue'
            |   }
            |}
            |$buildFileKMPConfigWithWasmJs
        """.trimMargin()
        )

        writeFlavorDevProperties()

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        // common should be expect (no @JsExport)
        val commonResult = buildKonfigFile(buildDir, "commonMain", "com.example", objectName = "AwesomeConfig")
        assertThat(commonResult.readText()).apply {
            doesNotContain("@JsExport")
            contains("expect object AwesomeConfig")
        }

        // js actual should have @JsExport
        val jsResult = buildKonfigFile(buildDir, "jsMain", "com.example", objectName = "AwesomeConfig")
        assertThat(jsResult.readText()).apply {
            contains("@JsExport")
            contains("@OptIn(ExperimentalJsExport::class)")
            contains("actual object AwesomeConfig")
        }

        // wasmJs actual should NOT have @JsExport
        val wasmJsResult = buildKonfigFile(buildDir, "wasmJsMain", "com.example", objectName = "AwesomeConfig")
        assertThat(wasmJsResult.readText()).apply {
            doesNotContain("@JsExport")
            doesNotContain("@OptIn(ExperimentalJsExport::class)")
            contains("actual object AwesomeConfig")
        }
    }

    @Test
    fun `The generated common object should not have JsExport when only wasmJs target exists`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |   exposeObjectWithName = "AwesomeConfig"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'stringValue', 'defaultValue'
            |   }
            |}
            |$buildFileKMPConfigWasmJsOnly
        """.trimMargin()
        )

        writeFlavorDevProperties()

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        val commonResult = buildKonfigFile(buildDir, "commonMain", "com.example", objectName = "AwesomeConfig")
        assertThat(commonResult.readText()).apply {
            doesNotContain("@JsExport")
            doesNotContain("@OptIn(ExperimentalJsExport::class)")
            contains("object AwesomeConfig")
        }
    }

    @Test
    fun `The generated target js object should have JsExport even when wasmJs target exists`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |   exposeObjectWithName = "AwesomeConfig"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'stringValue', 'defaultValue'
            |   }
            |   targetConfigs {
            |       js {
            |         buildConfigField 'STRING', 'stringValue', 'jsValue'
            |       }
            |   }
            |}
            |$buildFileKMPConfigWithWasmJs
        """.trimMargin()
        )

        writeFlavorDevProperties()

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        val jsResult = buildKonfigFile(buildDir, "jsMain", "com.example", objectName = "AwesomeConfig")
        assertThat(jsResult.readText()).apply {
            contains("@JsExport")
            contains("@OptIn(ExperimentalJsExport::class)")
            contains("object AwesomeConfig")
        }

        val wasmJsResult = buildKonfigFile(buildDir, "wasmJsMain", "com.example", objectName = "AwesomeConfig")
        assertThat(wasmJsResult.readText()).apply {
            doesNotContain("@JsExport")
            doesNotContain("@OptIn(ExperimentalJsExport::class)")
            contains("object AwesomeConfig")
        }
    }

    @Test
    fun `changing flavor between builds re-runs the task with the new flavor value`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'stringValue', 'defaultValue'
            |   }
            |   defaultConfigs("dev") {
            |       buildConfigField 'STRING', 'stringValue', 'devDefaultValue'
            |   }
            |   defaultConfigs("release") {
            |       buildConfigField 'STRING', 'stringValue', 'releaseDefaultValue'
            |   }
            |}
            |$buildFileKMPConfig
        """.trimMargin()
        )

        val buildDir = projectDir.buildKonfigDir()

        val runner = gradleRunner(projectDir)

        // First build with flavor=dev
        runner
            .withArguments("generateBuildKonfig", "--stacktrace", "-Pbuildkonfig.flavor=dev")
            .build()
            .assertBuildSuccessful()
        val devCommon = buildKonfigFile(buildDir, "commonMain", "com.example")
        assertThat(devCommon.readText())
            .contains("val stringValue: String = \"devDefaultValue\"")

        // Second build with flavor=release should NOT be UP-TO-DATE and must regenerate.
        val releaseResult = runner
            .withArguments("generateBuildKonfig", "--stacktrace", "-Pbuildkonfig.flavor=release")
            .build()
            .assertBuildSuccessful()

        assertThat(releaseResult.output).doesNotContain("generateBuildKonfig UP-TO-DATE")

        val releaseCommon = buildKonfigFile(buildDir, "commonMain", "com.example")
        assertThat(releaseCommon.readText())
            .contains("val stringValue: String = \"releaseDefaultValue\"")
    }
}
