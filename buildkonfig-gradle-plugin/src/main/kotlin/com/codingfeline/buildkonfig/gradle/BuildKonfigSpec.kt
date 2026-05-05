package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.compiler.BuildKonfigLogger
import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.compiler.TargetConfig
import com.codingfeline.buildkonfig.compiler.TargetName
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import java.io.Serializable

data class TargetConfigSource(
    val name: String,
    val configFile: TargetConfigInput,
    /**
     * Wires the generated source directory into the appropriate Kotlin source set.
     * Encapsulated as a callback so the call site does not need to know whether the
     * underlying source set comes from a KMP target, a standalone Kotlin/JVM project,
     * a standalone Kotlin/JS project, etc.
     */
    val registerSourceDir: (Provider<Directory>) -> Unit,
)

/**
 * Per-target task input. The output directory each target writes into is derived at task
 * action time from `BuildKonfigTask.outputDirectory`, so it is not part of the cache key
 * captured here.
 */
data class TargetConfigInput(
    @get:Input val targetName: TargetName,
    @get:Input val config: TargetConfig?,
) : Serializable

fun BuildKonfigExtension.mergeConfigs(
    logger: BuildKonfigLogger,
    flavor: Flavor = DEFAULT_FLAVOR,
    commonSourceSetName: String = COMMON_SOURCESET_NAME,
): Map<String, TargetConfig>? {
    if (!defaultConfigs.containsKey(DEFAULT_FLAVOR)) {
        logger.warn("BuildKonfig: non-flavored defaultConfigs is not provided. Skipping code generation.")
        return null
    }

    val defaultConfig = mergeDefaultConfigs(logger, flavor, defaultConfigs)

    val targetConfigsByName = mergeTargetConfigs(logger, flavor, targetConfigs.mapValues { it.value.toList() })

    // result Map has both common & target configs
    return targetConfigsByName
        .mapValues { (_, config) ->
            mergeConfigs(
                defaultConfig,
                checkTargetSpecificFields(defaultConfig, config)
            )
        } + (commonSourceSetName to defaultConfig)
}

fun checkTargetSpecificFields(defaultConfig: TargetConfig, targetConfig: TargetConfig): TargetConfig {
    val result = TargetConfig(targetConfig.name)

    val checked = targetConfig.fieldSpecs.mapValues { (name, spec) ->
        spec.copy(isTargetSpecific = !defaultConfig.fieldSpecs.containsKey(name))
    }

    result.fieldSpecs.putAll(checked)

    return result
}

fun mergeConfigs(
    base: TargetConfig,
    new: TargetConfig,
    onReplaced: (old: FieldSpec, new: FieldSpec) -> Unit = { _, _ -> /* no-op */ }
): TargetConfig {
    val result = TargetConfig(base.name)

    val fieldSpecs = listOf(base.fieldSpecs, new.fieldSpecs)
        .fold(mutableMapOf<String, FieldSpec>()) { acc, specs ->
            specs.forEach { (name, value) ->
                val alreadyPresent = acc[name]
                val newValue = value.copy()
                acc[name] = newValue
                if (alreadyPresent != null) {
                    onReplaced(alreadyPresent, newValue)
                }
            }
            acc
        }

    result.fieldSpecs.putAll(fieldSpecs)

    return result
}

fun mergeDefaultConfigs(
    logger: BuildKonfigLogger,
    flavor: Flavor,
    defaultConfigs: Map<Flavor, TargetConfig>
): TargetConfig {
    val default = defaultConfigs.getValue(DEFAULT_FLAVOR)
    val flavored = defaultConfigs[flavor]

    if (flavor == DEFAULT_FLAVOR || flavored == null) {
        return default.copy()
    }

    return mergeConfigs(default, flavored) { old, new ->
        logger.info("BuildKonfig(Default): field '${old.name}' is being replaced with flavored($flavor): ${old.value} -> ${new.value}")
    }
}

fun mergeTargetConfigs(
    logger: BuildKonfigLogger,
    flavor: Flavor, /* = kotlin.String */
    targetConfigs: Map<Flavor, List<TargetConfig>>
): Map<String, TargetConfig> {
    // default for targetConfigs
    val defaultTargetConfigs = targetConfigs
        .getOrDefault(DEFAULT_FLAVOR, emptyList())
        // convert to Map<name, TargetConfig>
        .associateBy { "${it.name}Main" }

    val flavoredConfigs = if (flavor != DEFAULT_FLAVOR) {
        targetConfigs.getOrDefault(flavor, emptyList())
            // convert to Map<name, TargetConfig>
            .associateBy { "${it.name}Main" }
    } else {
        // we don't want to merge the same configs
        emptyMap()
    }

    return listOf(defaultTargetConfigs, flavoredConfigs)
        .fold(mutableMapOf()) { acc, configs ->
            configs.forEach { (name, config) ->
                val alreadyPresent = acc[name]
                acc[name] = if (alreadyPresent != null) {
                    mergeConfigs(alreadyPresent, config) { old, new ->
                        logger.info("BuildKonfig($name): field is being replaced with flavored($flavor): ${old.value} -> ${new.value}")
                    }
                } else {
                    config.copy()
                }
            }
            acc
        }
}
