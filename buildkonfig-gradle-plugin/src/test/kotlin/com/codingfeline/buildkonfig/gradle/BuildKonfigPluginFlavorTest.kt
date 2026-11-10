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

    private fun writeFlavoredBuildFile(extraInside: String = "", trailing: String = "") {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |${extraInside.trimIndent()}
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
            |
            |${trailing.trimIndent()}
            """.trimMargin()
        )
    }

    private fun generateCommon(vararg extraArgs: String): String {
        val buildDir = projectDir.buildKonfigDir()
        gradleRunner(projectDir)
            .withArguments(listOf("generateBuildKonfig", "--stacktrace") + extraArgs)
            .build()
            .assertBuildSuccessful()
        return buildKonfigFile(buildDir, "commonMain", "com.example").readText()
    }

    private fun writeProperties(name: String, contents: String) {
        projectDir.newFile(name).writeText(contents)
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
                |@file:Suppress("REDUNDANT_VISIBILITY_MODIFIER")
                |
                |package com.example
                |
                |import kotlin.String
                |import kotlin.Suppress
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

    @Test
    fun `flavor can be obtained from a properties file`() {
        writeFlavoredBuildFile(
            extraInside = "flavorPropertiesFile = layout.projectDirectory.file('konfig.properties')",
        )
        writeProperties("konfig.properties", "buildkonfig.flavor=dev")

        assertThat(generateCommon())
            .contains("val stringValue: String = \"devDefaultValue\"")
    }

    @Test
    fun `flavor properties file overrides gradle properties`() {
        writeFlavoredBuildFile(
            extraInside = "flavorPropertiesFile.set(layout.projectDirectory.file('konfig.properties'))",
        )
        appendGradleProperties("buildkonfig.flavor=release")
        writeProperties("konfig.properties", "buildkonfig.flavor=dev")

        assertThat(generateCommon())
            .contains("val stringValue: String = \"devDefaultValue\"")
    }

    @Test
    fun `cli parameter overrides flavor properties file and gradle properties`() {
        writeFlavoredBuildFile(
            extraInside = """
                flavorPropertiesFile.set(layout.projectDirectory.file('konfig.properties'))
                defaultConfigs("ci") {
                    buildConfigField 'STRING', 'stringValue', 'ciValue'
                }
            """,
        )
        appendGradleProperties("buildkonfig.flavor=dev")
        writeProperties("konfig.properties", "buildkonfig.flavor=release")

        assertThat(generateCommon("-Pbuildkonfig.flavor=ci"))
            .contains("val stringValue: String = \"ciValue\"")
    }

    @Test
    fun `unset flavor properties file does not read local properties`() {
        writeFlavoredBuildFile()
        writeProperties("local.properties", "buildkonfig.flavor=dev")

        assertThat(generateCommon())
            .contains("val stringValue: String = \"defaultValue\"")
    }

    @Test
    fun `missing flavor properties file falls back to gradle properties`() {
        writeFlavoredBuildFile(
            extraInside = "flavorPropertiesFile.set(layout.projectDirectory.file('konfig.properties'))",
        )
        appendGradleProperties("buildkonfig.flavor=dev")

        assertThat(generateCommon())
            .contains("val stringValue: String = \"devDefaultValue\"")
    }

    @Test
    fun `flavor properties file without the key falls back to gradle properties`() {
        writeFlavoredBuildFile(
            extraInside = "flavorPropertiesFile.set(layout.projectDirectory.file('konfig.properties'))",
        )
        writeProperties("konfig.properties", "some.other=value")
        appendGradleProperties("buildkonfig.flavor=dev")

        assertThat(generateCommon())
            .contains("val stringValue: String = \"devDefaultValue\"")
    }

    @Test
    fun `flavor properties file without the key and without project properties uses defaults`() {
        writeFlavoredBuildFile(
            extraInside = "flavorPropertiesFile.set(layout.projectDirectory.file('konfig.properties'))",
        )
        writeProperties("konfig.properties", "some.other=value")

        assertThat(generateCommon())
            .contains("val stringValue: String = \"defaultValue\"")
    }

    @Test
    fun `empty flavor in properties file selects non-flavored defaults`() {
        writeFlavoredBuildFile(
            extraInside = "flavorPropertiesFile.set(layout.projectDirectory.file('konfig.properties'))",
        )
        writeProperties("konfig.properties", "buildkonfig.flavor=")
        appendGradleProperties("buildkonfig.flavor=dev")

        assertThat(generateCommon())
            .contains("val stringValue: String = \"defaultValue\"")
    }

    @Test
    fun `unrelated keys in flavor properties file do not become project properties`() {
        writeFlavoredBuildFile(
            extraInside = "flavorPropertiesFile.set(layout.projectDirectory.file('konfig.properties'))",
            trailing = """
                afterEvaluate {
                    if (findProperty('some.unrelated') != null) {
                        throw new GradleException('unrelated key leaked: ' + findProperty('some.unrelated'))
                    }
                }
            """,
        )
        writeProperties(
            "konfig.properties",
            """
            some.unrelated=leaked
            buildkonfig.flavor=dev
            """.trimIndent(),
        )

        assertThat(generateCommon())
            .contains("val stringValue: String = \"devDefaultValue\"")
    }

    @Test
    fun `script-defined flavor is used when flavor properties file is unset`() {
        writeFlavoredBuildFile(
            trailing = "extensions.extraProperties.set('buildkonfig.flavor', 'dev')",
        )

        assertThat(generateCommon())
            .contains("val stringValue: String = \"devDefaultValue\"")
    }

    @Test
    fun `script-defined flavor is used when flavor properties file lacks the key`() {
        writeFlavoredBuildFile(
            extraInside = "flavorPropertiesFile.set(layout.projectDirectory.file('konfig.properties'))",
            trailing = "extensions.extraProperties.set('buildkonfig.flavor', 'dev')",
        )
        writeProperties("konfig.properties", "some.other=value")

        assertThat(generateCommon())
            .contains("val stringValue: String = \"devDefaultValue\"")
    }

    @Test
    fun `malformed flavor properties file fails with the file path`() {
        writeFlavoredBuildFile(
            extraInside = "flavorPropertiesFile.set(layout.projectDirectory.file('broken.properties'))",
        )
        writeProperties("broken.properties", "buildkonfig.flavor=\\uZZZZ")

        val result = gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .buildAndFail()

        assertThat(result.output).contains("Failed to read BuildKonfig flavor properties file")
        assertThat(result.output).contains("broken.properties")
    }

    @Test
    fun `flavor properties file that is a directory fails with the path`() {
        writeFlavoredBuildFile(
            extraInside = "flavorPropertiesFile.set(layout.projectDirectory.file('flavor-dir'))",
        )
        projectDir.newFolder("flavor-dir")

        val result = gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .buildAndFail()

        assertThat(result.output).contains("Failed to read BuildKonfig flavor properties file")
        assertThat(result.output).contains("flavor-dir")
        assertThat(result.output).contains("directory")
    }

    @Test
    fun `cli flavor bypasses an unreadable flavor properties file`() {
        writeFlavoredBuildFile(
            extraInside = "flavorPropertiesFile.set(layout.projectDirectory.file('broken.properties'))",
        )
        writeProperties("broken.properties", "buildkonfig.flavor=\\uZZZZ")

        // Reading the malformed file would fail the build, so success proves it was skipped.
        assertThat(generateCommon("-Pbuildkonfig.flavor=release"))
            .contains("val stringValue: String = \"releaseDefaultValue\"")
    }

    @Test
    fun `empty cli flavor overrides flavor properties file`() {
        writeFlavoredBuildFile(
            extraInside = "flavorPropertiesFile.set(layout.projectDirectory.file('konfig.properties'))",
        )
        writeProperties("konfig.properties", "buildkonfig.flavor=dev")

        assertThat(generateCommon("-Pbuildkonfig.flavor="))
            .contains("val stringValue: String = \"defaultValue\"")
    }

    @Test
    fun `system property flavor overrides flavor properties file and gradle properties`() {
        writeFlavoredBuildFile(
            extraInside = "flavorPropertiesFile.set(layout.projectDirectory.file('konfig.properties'))",
        )
        appendGradleProperties("buildkonfig.flavor=dev")
        writeProperties("konfig.properties", "buildkonfig.flavor=dev")

        assertThat(generateCommon("-Dorg.gradle.project.buildkonfig.flavor=release"))
            .contains("val stringValue: String = \"releaseDefaultValue\"")
    }

    @Test
    fun `environment variable flavor overrides flavor properties file and gradle properties`() {
        writeFlavoredBuildFile(
            extraInside = "flavorPropertiesFile.set(layout.projectDirectory.file('konfig.properties'))",
        )
        appendGradleProperties("buildkonfig.flavor=dev")
        writeProperties("konfig.properties", "buildkonfig.flavor=dev")

        val buildDir = projectDir.buildKonfigDir()
        gradleRunner(projectDir)
            // withEnvironment replaces the whole environment, so keep the inherited variables.
            .withEnvironment(System.getenv() + ("ORG_GRADLE_PROJECT_buildkonfig.flavor" to "release"))
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        assertThat(buildKonfigFile(buildDir, "commonMain", "com.example").readText())
            .contains("val stringValue: String = \"releaseDefaultValue\"")
    }

    @Test
    fun `cli flavor overrides script-defined flavor when flavor properties file is unset`() {
        writeFlavoredBuildFile(
            trailing = "extensions.extraProperties.set('buildkonfig.flavor', 'dev')",
        )

        assertThat(generateCommon("-Pbuildkonfig.flavor=release"))
            .contains("val stringValue: String = \"releaseDefaultValue\"")
    }
}
