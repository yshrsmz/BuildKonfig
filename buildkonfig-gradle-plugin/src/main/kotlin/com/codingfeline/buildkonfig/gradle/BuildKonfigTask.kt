package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.VERSION
import com.codingfeline.buildkonfig.compiler.BuildKonfigData
import com.codingfeline.buildkonfig.compiler.BuildKonfigEnvironment
import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.compiler.TargetConfig
import com.codingfeline.buildkonfig.compiler.TargetConfigFile
import com.codingfeline.buildkonfig.compiler.TargetName
import org.gradle.api.DefaultTask
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.File

const val FLAVOR_PROPERTY = "buildkonfig.flavor"

open class BuildKonfigTask : DefaultTask() {

    // Required to invalidate the task on version updates.
    @Suppress("unused")
    @get:Input
    val pluginVersion: String
        get() = VERSION

    @Input
    lateinit var packageName: String

    @get:Input
    val targetNames: Set<TargetName>
        get() = outputDirectories.keys

    @Internal
    lateinit var extension: BuildKonfigExtension

    @get:Input
    val defaultConfigs: Map<String, TargetConfig>
        get() = extension.defaultConfigs.mapValues { (_, value) -> value.toTargetConfig() }

    @get:Input
    val targetConfigs: Map<String, List<TargetConfig>>
        get() = extension.targetConfigs.mapValues { (_, value) -> value.map { it.toTargetConfig() } }

    @get:Input
    val flavor: String
        get() = findFlavor()


    @Suppress("unused")
    @get:OutputDirectories
    val targetOutputDirectories: List<File>
        get() = outputDirectories.values.toList()


    @OutputDirectory
    lateinit var commonOutputDirectory: File

    @Internal
    lateinit var outputDirectories: Map<TargetName, File>

    @Suppress("unused")
    @TaskAction
    fun generateBuildKonfigFiles() {
        val flavorName = flavor

        if (!defaultConfigs.containsKey("")) {
            throw IllegalStateException("non flavored defaultConfigs must be provided")
        }

        // clean up output directories
        targetOutputDirectories.forEach {
            it.deleteRecursively()
            it.mkdirs()
        }

        val defaultConfig = getMergedDefaultConfig(flavorName)

        val mergedConfigFiles = targetNames.map { targetName ->
            val sortedConfigs = mutableListOf<TargetConfig>()

            // get non-flavored config first
            targetConfigs.getOrDefault("", emptyList()).filter { it.name == targetName.name }
                .let { sortedConfigs.addAll(it) }
            // get flavored config
            targetConfigs.getOrDefault(flavorName, emptyList()).filter { it.name == targetName.name }
                .let { sortedConfigs.addAll(it) }

            val defaultConfigsForTarget = defaultConfig
                .let { base ->
                    TargetConfig(targetName.name)
                        .also { it.fieldSpecs.putAll(base.fieldSpecs) }
                }

            sortedConfigs
                .fold(defaultConfigsForTarget) { previous, current ->
                    mergeConfigs(
                        targetName.name,
                        defaultConfigsForTarget,
                        previous,
                        current
                    )
                }
                .let { TargetConfigFile(targetName.platformType, outputDirectories[targetName]!!, it) }
        }

        val data = BuildKonfigData(
            packageName = packageName,
            commonConfig = TargetConfigFile(KotlinPlatformType.common.name, commonOutputDirectory, defaultConfig),
            targetConfigs = mergedConfigFiles
        )

        BuildKonfigEnvironment(data).generateConfigs { info -> logger.log(LogLevel.INFO, info) }
    }

    private fun mergeDefaultConfigs(
        flavor: String,
        baseConfig: TargetConfig,
        newConfig: TargetConfig?
    ): TargetConfig {
        val result = TargetConfig(baseConfig.name)

        listOf(
            baseConfig.fieldSpecs,
            newConfig?.fieldSpecs ?: emptyMap<String, FieldSpec>()
        ).forEach { specs ->
            specs.forEach { name, value ->
                val alreadyPresent = result.fieldSpecs[name]
                if (alreadyPresent != null) {
                    logger.info("Default BuildKonfig: buildConfigField '$name' is being replaced with flavored($flavor): ${alreadyPresent.value} -> ${value.value}")
                }
                result.fieldSpecs[name] = value.copy()
            }
        }

        return result
    }

    private fun mergeConfigs(
        targetName: String,
        defaultConfig: TargetConfig,
        baseConfig: TargetConfig,
        newConfig: TargetConfig
    ): TargetConfig {
        val result = TargetConfig(targetName)

        baseConfig.fieldSpecs.forEach { name, value ->
            result.fieldSpecs[name] = value.copy(isTargetSpecific = !defaultConfig.fieldSpecs.contains(name))
        }

        newConfig.fieldSpecs.forEach { name, value ->
            val alreadyPresent = result.fieldSpecs[name]
            if (alreadyPresent != null) {
                logger.info("BuildKonfig for $targetName: buildConfigField '$name' is being replaced: ${alreadyPresent.value} -> ${value.value}")
            }
            result.fieldSpecs[name] = value.copy(isTargetSpecific = !defaultConfig.fieldSpecs.contains(name))
        }

        return result
    }

    private fun findFlavor(): String {
        val flavor = project.findProperty(FLAVOR_PROPERTY) ?: ""
        if (flavor is String) {
            return flavor
        } else {
            logger.error("$FLAVOR_PROPERTY must be string. Fallback to non-flavored config: ${flavor::class.java}")
            return ""
        }
    }

    private fun getMergedDefaultConfig(flavor: String): TargetConfig {
        val default = defaultConfigs.getValue("")
        val flavored = defaultConfigs[flavor]

        return mergeDefaultConfigs(flavor, default, flavored)
    }
}
