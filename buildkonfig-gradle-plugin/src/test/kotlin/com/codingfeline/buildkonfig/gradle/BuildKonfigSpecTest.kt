package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.compiler.BuildKonfigLogger
import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.compiler.LogLevel
import com.codingfeline.buildkonfig.compiler.TargetConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BuildKonfigSpecTest {

    @Test
    fun `mergeConfigs - non-colliding fields are unioned and onReplaced is not invoked`() {
        val base = targetConfig("common", stringField("a", "1"))
        val new = targetConfig("common", stringField("b", "2"))
        val replaced = mutableListOf<Pair<FieldSpec, FieldSpec>>()

        val result = mergeConfigs(base, new) { old, n -> replaced += old to n }

        assertThat(result.fieldSpecs.keys).containsExactly("a", "b")
        assertThat(result.fieldSpecs["a"]?.value).isEqualTo("1")
        assertThat(result.fieldSpecs["b"]?.value).isEqualTo("2")
        assertThat(replaced).isEmpty()
    }

    @Test
    fun `mergeConfigs - new field overrides base on collision and onReplaced is invoked`() {
        val base = targetConfig("common", stringField("key", "base"))
        val new = targetConfig("common", stringField("key", "new"))
        val replaced = mutableListOf<Pair<FieldSpec, FieldSpec>>()

        val result = mergeConfigs(base, new) { old, n -> replaced += old to n }

        assertThat(result.fieldSpecs["key"]?.value).isEqualTo("new")
        assertThat(replaced).hasSize(1)
        assertThat(replaced.single().first.value).isEqualTo("base")
        assertThat(replaced.single().second.value).isEqualTo("new")
    }

    @Test
    fun `mergeDefaultConfigs - returns default copy when flavor is DEFAULT_FLAVOR`() {
        val defaults = mapOf(DEFAULT_FLAVOR to targetConfig("defaults", stringField("a", "1")))
        val logger = RecordingLogger()

        val merged = mergeDefaultConfigs(logger, DEFAULT_FLAVOR, defaults)

        assertThat(merged.fieldSpecs["a"]?.value).isEqualTo("1")
        assertThat(logger.messages).isEmpty()
    }

    @Test
    fun `mergeDefaultConfigs - returns default copy when flavor has no entry`() {
        val defaults = mapOf(DEFAULT_FLAVOR to targetConfig("defaults", stringField("a", "1")))
        val logger = RecordingLogger()

        val merged = mergeDefaultConfigs(logger, "nonexistent", defaults)

        assertThat(merged.fieldSpecs["a"]?.value).isEqualTo("1")
        assertThat(logger.messages).isEmpty()
    }

    @Test
    fun `mergeDefaultConfigs - flavored defaults override unflavored on collision and log info`() {
        val defaults = mapOf(
            DEFAULT_FLAVOR to targetConfig("defaults", stringField("env", "prod")),
            "dev" to targetConfig("defaults", stringField("env", "dev")),
        )
        val logger = RecordingLogger()

        val merged = mergeDefaultConfigs(logger, "dev", defaults)

        assertThat(merged.fieldSpecs["env"]?.value).isEqualTo("dev")
        val info = logger.messages.filter { it.first == LogLevel.INFO }.map { it.second }
        assertThat(info).isNotEmpty()
        assertThat(info.first()).contains("BuildKonfig(Default)")
        assertThat(info.first()).contains("env")
        assertThat(info.first()).contains("flavored(dev)")
    }

    @Test
    fun `mergeTargetConfigs - keys results by sourceSet name`() {
        val targets = mapOf(
            DEFAULT_FLAVOR to listOf(
                targetConfig("jvm", stringField("a", "1")),
                targetConfig("iosX64", stringField("b", "2")),
            ),
        )
        val logger = RecordingLogger()

        val merged = mergeTargetConfigs(logger, DEFAULT_FLAVOR, targets)

        assertThat(merged.keys).containsExactly("jvmMain", "iosX64Main")
        assertThat(merged["jvmMain"]?.fieldSpecs?.get("a")?.value).isEqualTo("1")
        assertThat(merged["iosX64Main"]?.fieldSpecs?.get("b")?.value).isEqualTo("2")
    }

    @Test
    fun `mergeTargetConfigs - flavored target overrides non-flavored on collision and log info`() {
        val targets = mapOf(
            DEFAULT_FLAVOR to listOf(targetConfig("jvm", stringField("env", "default"))),
            "dev" to listOf(targetConfig("jvm", stringField("env", "dev"))),
        )
        val logger = RecordingLogger()

        val merged = mergeTargetConfigs(logger, "dev", targets)

        assertThat(merged["jvmMain"]?.fieldSpecs?.get("env")?.value).isEqualTo("dev")
        val info = logger.messages.filter { it.first == LogLevel.INFO }.map { it.second }
        assertThat(info).isNotEmpty()
        assertThat(info.first()).contains("BuildKonfig(jvmMain)")
        assertThat(info.first()).contains("flavored(dev)")
    }

    @Test
    fun `mergeTargetConfigs - flavor with no entry returns only the default targets`() {
        val targets = mapOf(
            DEFAULT_FLAVOR to listOf(targetConfig("jvm", stringField("env", "default"))),
        )
        val logger = RecordingLogger()

        val merged = mergeTargetConfigs(logger, "nonexistent", targets)

        assertThat(merged.keys).containsExactly("jvmMain")
        assertThat(merged["jvmMain"]?.fieldSpecs?.get("env")?.value).isEqualTo("default")
    }

    @Test
    fun `checkTargetSpecificFields - fields absent from default are marked target-specific`() {
        val default = targetConfig("defaults", stringField("shared", "x"))
        val target = targetConfig(
            "jvm",
            stringField("shared", "x"),
            stringField("jvmOnly", "y"),
        )

        val checked = checkTargetSpecificFields(default, target)

        assertThat(checked.fieldSpecs["shared"]?.isTargetSpecific).isFalse()
        assertThat(checked.fieldSpecs["jvmOnly"]?.isTargetSpecific).isTrue()
    }

    @Test
    fun `checkTargetSpecificFields - empty default config marks every target field as target-specific`() {
        val default = targetConfig("defaults")
        val target = targetConfig("jvm", stringField("a", "1"), stringField("b", "2"))

        val checked = checkTargetSpecificFields(default, target)

        assertThat(checked.fieldSpecs.values.all { it.isTargetSpecific }).isTrue()
    }

    private companion object {

        fun stringField(name: String, value: String?): FieldSpec =
            FieldSpec(FieldSpec.Type.STRING, name, value)

        fun targetConfig(name: String, vararg fields: FieldSpec): TargetConfig =
            TargetConfig(name).also { config ->
                fields.forEach { config.fieldSpecs[it.name] = it }
            }

        class RecordingLogger : BuildKonfigLogger {
            val messages = mutableListOf<Pair<LogLevel, String>>()
            override fun log(level: LogLevel, message: String) {
                messages.add(level to message)
            }
        }
    }
}
