package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.compiler.TargetConfig
import com.codingfeline.buildkonfig.compiler.TargetConfigFile
import com.codingfeline.buildkonfig.compiler.TargetName
import com.codingfeline.buildkonfig.gradle.kotlin.Source
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

data class TargetConfigSource(
    val configFile: TargetConfigFileImpl,
    val sourceSet: KotlinSourceSet,
    val source: Source
)

data class TargetConfigFileImpl(
    @Input override val targetName: TargetName,
    @Internal override val outputDirectory: File,
    @Input override val config: TargetConfig?
) : TargetConfigFile

fun BuildKonfigExtension.mergeConfigs(
    logger: (String) -> Unit,
    flavor: Flavor = DEFAULT_FLAVOR
): Map<String, TargetConfig> {
    if (!defaultConfigs.containsKey(DEFAULT_FLAVOR)) {
        throw IllegalStateException("non-flavored defaultConfigs must be provided.")
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
        } + ("common" to defaultConfig)
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
    logger: (String) -> Unit,
    flavor: Flavor,
    defaultConfigs: Map<Flavor, TargetConfig>
): TargetConfig {
    val default = defaultConfigs.getValue(DEFAULT_FLAVOR)
    val flavored = defaultConfigs[flavor]

    if (flavor == DEFAULT_FLAVOR || flavored == null) {
        return default.copy()
    }

    return mergeConfigs(default, flavored) { old, new ->
        logger("BuildKonfig(Default): field '${old.name}' is being replaced with flavored($flavor): ${old.value} -> ${new.value}")
    }
}

fun mergeTargetConfigs(
    logger: (String) -> Unit,
    flavor: Flavor, /* = kotlin.String */
    targetConfigs: Map<Flavor, List<TargetConfig>>
): Map<String, TargetConfig> {
    // default for targetConfigs
    val defaultTargetConfigs = targetConfigs
        .getOrDefault(DEFAULT_FLAVOR, emptyList())
        // convert to Map<name, TargetConfig>
        .associateBy { it.name }

    val flavoredConfigs = if (flavor !== DEFAULT_FLAVOR) {
        targetConfigs.getOrDefault(flavor, emptyList())
            // convert to Map<name, TargetConfig>
            .associateBy { it.name }
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
                        logger("BuildKonfig($name): field is being replaced with flavored($flavor): ${old.value} -> ${new.value}")
                    }
                } else {
                    config.copy()
                }
            }
            acc
        }
}
