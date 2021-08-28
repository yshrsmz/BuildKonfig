package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.VERSION
import com.codingfeline.buildkonfig.compiler.TargetConfig
import com.codingfeline.buildkonfig.compiler.TargetConfigFile
import com.codingfeline.buildkonfig.compiler.TargetName
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class BuildKonfigTask2 : DefaultTask() {

    @Suppress("unused")
    @Input
    val pluginVersion = VERSION

    @get:OutputDirectory
    lateinit var outputDirectory: File

    @Nested
    lateinit var objectProperties: BuildKonfigObjectPropertiesImpl

    @get:Input
    lateinit var target: TargetName

    @get:Input
    lateinit var defaultConfigs: Map<String, TargetConfig>

    @get:Input
    lateinit var targetConfigs: Map<String, TargetConfig>

    @Suppress("unused")
    @get:Input
    val flavor: String
        get() = findFlavor()

    @TaskAction
    fun generateBuildKonfigFiles() {
        val flavorName = flavor

        outputDirectory.deleteRecursively()
        outputDirectory.mkdirs()

        val defaultConfig = getMergedDefaultConfig(flavorName)
        val targetConfigs = getTargetConfigs(flavorName)
        val result = targetConfigs.fold(defaultConfig) { acc, target ->
            mergeConfigs(defaultConfig, acc, target)
        }
        TargetConfigFile(
            targetName = target,
            outputDirectory = outputDirectory,
            result
        )
    }

    private fun findFlavor(): String {
        return when (val flavor = project.findProperty(FLAVOR_PROPERTY)) {
            flavor == null -> ""
            flavor is String -> flavor as String
            else -> {
                logger.error(
                    "$FLAVOR_PROPERTY must be a String. " +
                            "Fallback to non-flavored config: ${flavor!!::class.java}"
                )
                ""
            }
        }
    }

    private fun getMergedDefaultConfig(flavor: String): TargetConfig {
        // default should always be provided
        val default = defaultConfigs.getValue("")
        val flavored = defaultConfigs[flavor]

        return mergeDefaultConfigs(flavor, default, flavored)
    }

    private fun getTargetConfigs(flavor: String): List<TargetConfig> {
        val default = targetConfigs.getOrDefault("", null)
        val flavored = targetConfigs.getOrDefault(flavor, null)

        return listOfNotNull(default, flavored)
    }

    private fun mergeDefaultConfigs(
        flavor: String,
        baseConfig: TargetConfig,
        newConfig: TargetConfig?
    ): TargetConfig {
        val result = TargetConfig(baseConfig.name)

        listOf(baseConfig.fieldSpecs, newConfig?.fieldSpecs ?: emptyMap()).forEach { specs ->
            specs.forEach { (name, spec) ->
                val alreadyPresent = result.fieldSpecs[name]
                if (alreadyPresent != null) {
                    logger.info(
                        "Default BuildKonfig: buildConfigField '$name' is being replaced with flavored($flavor): " +
                                "${alreadyPresent.value} -> ${spec.value}"
                    )
                }
                result.fieldSpecs[name] = spec.copy()
            }
        }
        return result
    }

    private fun mergeConfigs(
        defaultConfig: TargetConfig,
        baseConfig: TargetConfig,
        newConfig: TargetConfig
    ): TargetConfig {
        val result = TargetConfig(baseConfig.name)

        baseConfig.fieldSpecs.forEach { (name, spec) ->
            result.fieldSpecs[name] = spec.copy(isTargetSpecific = !defaultConfig.fieldSpecs.containsKey(name))
        }

        newConfig.fieldSpecs.forEach { (name, spec) ->
            val alreadyPresent = result.fieldSpecs[name]
            if (alreadyPresent != null) {
                logger.info(
                    "BuildKonfig for ${target.name}: buildConfigField '$name' is being replaced: " +
                            "${alreadyPresent.value} -> ${spec.value}"
                )
            }
            result.fieldSpecs[name] = spec.copy(isTargetSpecific = !defaultConfig.fieldSpecs.containsKey(name))
        }
        return result
    }


    private companion object {
        const val FLAVOR_PROPERTY = "buildkonfig.flavor"
    }
}
