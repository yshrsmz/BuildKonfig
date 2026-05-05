package com.codingfeline.buildkonfig.compiler

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BuildKonfigEnvironmentTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `single concrete object is generated when no target configs are present`() {
        val outputDir = tempFolder.newFolder("commonMain")
        val data = BuildKonfigData(
            packageName = "com.example",
            objectName = "BuildKonfig",
            exposeObject = false,
            commonConfig = configFile(
                name = COMMON_SOURCESET_NAME,
                platformType = PlatformType.common,
                outputDirectory = outputDir,
                fields = listOf(stringField("foo", "bar")),
            ),
            targetConfigs = emptyList(),
            hasJsTarget = false,
        )

        val logger = RecordingLogger()
        val status = BuildKonfigEnvironment(data).generateConfigs(logger)

        assertThat(status).isInstanceOf(BuildKonfigEnvironment.CompilationStatus.Success::class.java)

        val generated = File(outputDir, "com/example/BuildKonfig.kt")
        assertThat(generated.exists()).isTrue()

        val content = generated.readText()
        assertThat(content).contains("internal object BuildKonfig")
        assertThat(content).contains("public val foo: String = \"bar\"")
        assertThat(content).doesNotContain("expect ")
        assertThat(content).doesNotContain("actual ")
    }

    @Test
    fun `expect-actual pair is generated when target configs are present`() {
        val commonDir = tempFolder.newFolder("commonMain")
        val jvmDir = tempFolder.newFolder("jvmMain")

        val data = BuildKonfigData(
            packageName = "com.example",
            objectName = "BuildKonfig",
            exposeObject = false,
            commonConfig = configFile(
                name = COMMON_SOURCESET_NAME,
                platformType = PlatformType.common,
                outputDirectory = commonDir,
                fields = listOf(stringField("env", "default")),
            ),
            targetConfigs = listOf(
                configFile(
                    name = "jvmMain",
                    platformType = PlatformType.jvm,
                    outputDirectory = jvmDir,
                    fields = listOf(stringField("env", "jvm")),
                ),
            ),
            hasJsTarget = false,
        )

        val status = BuildKonfigEnvironment(data).generateConfigs(RecordingLogger())
        assertThat(status).isInstanceOf(BuildKonfigEnvironment.CompilationStatus.Success::class.java)

        val commonFile = File(commonDir, "com/example/BuildKonfig.kt")
        assertThat(commonFile.readText()).apply {
            contains("internal expect object BuildKonfig")
            contains("public val env: String")
            doesNotContain("actual ")
        }

        val jvmFile = File(jvmDir, "com/example/BuildKonfig.kt")
        assertThat(jvmFile.readText()).apply {
            contains("internal actual object BuildKonfig")
            contains("actual val env: String = \"jvm\"")
            doesNotContain("expect ")
        }
    }

    @Test
    fun `nullable and const fields are emitted with the right modifiers`() {
        val outputDir = tempFolder.newFolder("commonMain")
        val data = BuildKonfigData(
            packageName = "com.example",
            objectName = "BuildKonfig",
            exposeObject = false,
            commonConfig = configFile(
                name = COMMON_SOURCESET_NAME,
                platformType = PlatformType.common,
                outputDirectory = outputDir,
                fields = listOf(
                    FieldSpec(FieldSpec.Type.STRING, "nullableString", null, nullable = true),
                    FieldSpec(FieldSpec.Type.STRING, "constString", "value", const = true),
                    FieldSpec(FieldSpec.Type.INT, "regularInt", "42"),
                ),
            ),
            targetConfigs = emptyList(),
            hasJsTarget = false,
        )

        BuildKonfigEnvironment(data).generateConfigs(RecordingLogger())

        val content = File(outputDir, "com/example/BuildKonfig.kt").readText()
        assertThat(content).contains("public val nullableString: String? = null")
        assertThat(content).contains("public const val constString: String = \"value\"")
        assertThat(content).contains("public val regularInt: Int = 42")
    }

    @Test
    fun `exposeObjectWithName plus a JS target adds @JsExport`() {
        val outputDir = tempFolder.newFolder("commonMain")
        val data = BuildKonfigData(
            packageName = "com.example",
            objectName = "BuildKonfig",
            exposeObject = true,
            commonConfig = configFile(
                name = COMMON_SOURCESET_NAME,
                platformType = PlatformType.common,
                outputDirectory = outputDir,
                fields = listOf(stringField("foo", "bar")),
            ),
            targetConfigs = emptyList(),
            hasJsTarget = true,
        )

        BuildKonfigEnvironment(data).generateConfigs(RecordingLogger())

        val content = File(outputDir, "com/example/BuildKonfig.kt").readText()
        assertThat(content).contains("@JsExport")
        assertThat(content).contains("@OptIn(ExperimentalJsExport::class)")
        assertThat(content).contains("public object BuildKonfig")
    }

    @Test
    fun `exposeObjectWithName without a JS target does not add @JsExport`() {
        val outputDir = tempFolder.newFolder("commonMain")
        val data = BuildKonfigData(
            packageName = "com.example",
            objectName = "BuildKonfig",
            exposeObject = true,
            commonConfig = configFile(
                name = COMMON_SOURCESET_NAME,
                platformType = PlatformType.common,
                outputDirectory = outputDir,
                fields = listOf(stringField("foo", "bar")),
            ),
            targetConfigs = emptyList(),
            hasJsTarget = false,
        )

        BuildKonfigEnvironment(data).generateConfigs(RecordingLogger())

        val content = File(outputDir, "com/example/BuildKonfig.kt").readText()
        assertThat(content).doesNotContain("@JsExport")
        assertThat(content).doesNotContain("@OptIn(ExperimentalJsExport::class)")
        assertThat(content).contains("public object BuildKonfig")
    }

    @Test
    fun `const fields warn when target-specific configs are present (K2 limitation)`() {
        val commonDir = tempFolder.newFolder("commonMain")
        val jvmDir = tempFolder.newFolder("jvmMain")

        val data = BuildKonfigData(
            packageName = "com.example",
            objectName = "BuildKonfig",
            exposeObject = false,
            commonConfig = configFile(
                name = COMMON_SOURCESET_NAME,
                platformType = PlatformType.common,
                outputDirectory = commonDir,
                fields = listOf(
                    FieldSpec(FieldSpec.Type.STRING, "constField", "value", const = true),
                ),
            ),
            targetConfigs = listOf(
                configFile(
                    name = "jvmMain",
                    platformType = PlatformType.jvm,
                    outputDirectory = jvmDir,
                    fields = listOf(stringField("env", "jvm")),
                ),
            ),
            hasJsTarget = false,
        )

        val logger = RecordingLogger()
        BuildKonfigEnvironment(data).generateConfigs(logger)

        val warnings = logger.messages.filter { it.first == LogLevel.WARN }.map { it.second }
        assertThat(warnings).isNotEmpty()
        assertThat(warnings.first()).contains("[constField]")
        assertThat(warnings.first()).contains("declared with `const = true`")

        // Common file emits `val` (not `const val`) due to the K2 expect-side restriction;
        // each target keeps `actual const val`.
        val commonContent = File(commonDir, "com/example/BuildKonfig.kt").readText()
        assertThat(commonContent).contains("public val constField: String")
        assertThat(commonContent).doesNotContain("const val constField")
    }

    private companion object {

        const val COMMON_SOURCESET_NAME = "commonMain"

        fun stringField(name: String, value: String): FieldSpec =
            FieldSpec(FieldSpec.Type.STRING, name, value)

        fun configFile(
            name: String,
            platformType: PlatformType,
            outputDirectory: File,
            fields: List<FieldSpec>,
        ): TargetConfigFile = TestTargetConfigFile(
            targetName = TargetName(name, platformType),
            outputDirectory = outputDirectory,
            config = TargetConfig(name).apply {
                fields.forEach { fieldSpecs[it.name] = it }
            },
        )

        private data class TestTargetConfigFile(
            override val targetName: TargetName,
            override val outputDirectory: File,
            override val config: TargetConfig?,
        ) : TargetConfigFile

        class RecordingLogger : BuildKonfigLogger {
            val messages = mutableListOf<Pair<LogLevel, String>>()
            override fun log(level: LogLevel, message: String) {
                messages.add(level to message)
            }
        }
    }
}
