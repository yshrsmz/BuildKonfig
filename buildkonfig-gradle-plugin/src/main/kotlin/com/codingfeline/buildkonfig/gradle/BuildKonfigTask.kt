package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.VERSION
import com.codingfeline.buildkonfig.compiler.BuildKonfigData
import com.codingfeline.buildkonfig.compiler.BuildKonfigEnvironment
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
    val defaultConfig: TargetConfig?
        get() = extension.defaultConfigs?.toPlatformConfig()


    @get:Input
    val targetConfigs: List<TargetConfig>
        get() = extension.targetConfigs?.map { it.toPlatformConfig() } ?: emptyList()


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

        println("flavor: ${getFlavor()}")

        val defaultConfig = defaultConfig ?: throw IllegalStateException("defaultConfigs must be provided")

        val mergedConfigFiles = targetNames.map { targetName ->
            val sortedConfigs = mutableListOf<TargetConfig>()
            sortedConfigs.addAll(targetConfigs.filter { it.name == targetName.name })
            sortedConfigs.addAll(targetConfigs.filter { it.name == "${targetName.name}Main" })

            val defaultConfigsForTarget = TargetConfig(targetName.name)
                .apply {
                    this.fieldSpecs.putAll(defaultConfig.fieldSpecs)
                }

            sortedConfigs
                .fold(defaultConfigsForTarget) { previous, current ->
                    mergeConfigs(
                        targetName.name,
                        defaultConfig,
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

    fun mergeConfigs(
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

    private fun getFlavor(): String {
        val flavor = project.findProperty(FLAVOR_PROPERTY) ?: ""
        if (flavor is String) {
            return flavor
        } else {
            throw IllegalStateException("$FLAVOR_PROPERTY must be String")
        }
    }
}
