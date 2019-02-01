package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.VERSION
import com.codingfeline.buildkonfig.compiler.BuildKonfigData
import com.codingfeline.buildkonfig.compiler.BuildKonfigEnvironment
import com.codingfeline.buildkonfig.compiler.CompilationType
import com.codingfeline.buildkonfig.compiler.TargetConfig
import com.codingfeline.buildkonfig.compiler.TargetConfigFile
import com.codingfeline.buildkonfig.compiler.TargetName
import org.gradle.api.DefaultTask
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.File

open class BuildKonfigTask : DefaultTask() {

    // Required to invalidate the task on version updates.
    @get:Input
    val pluginVersion: String
        get() = VERSION


    @Input
    lateinit var compilationType: CompilationType

    @Input
    lateinit var packageName: String

    @get:Input
    val targetNames: Set<TargetName>
        get() = outputDirectories.keys

    lateinit var extension: BuildKonfigExtension


    @get:Input
    val defaultConfig: TargetConfig?
        get() = extension.defaultConfigs?.toPlatformConfig()


    @get:Input
    val targetConfigs: List<TargetConfig>
        get() = extension.targetConfigs?.map { it.toPlatformConfig() } ?: emptyList()


    @get:OutputDirectories
    val targetOutputDirectories: List<File>
        get() = outputDirectories.values.toList()


    @OutputDirectory
    lateinit var commonOutputDirectory: File

    lateinit var outputDirectories: Map<TargetName, File>

    @TaskAction
    fun generateBuildKonfigFiles() {

        val defaultConfig = defaultConfig ?: throw IllegalStateException("defaultConfigs must be provided")

        val mergedConfigFiles = targetNames.map { targetName ->
            println("merging configs for $targetName")
            val sortedConfigs = mutableListOf<TargetConfig>()
            sortedConfigs.addAll(targetConfigs.filter { it.name == targetName.name })
            sortedConfigs.addAll(targetConfigs.filter { it.name == "${targetName}Main" })

            if (compilationType == CompilationType.TEST) {
                sortedConfigs.addAll(targetConfigs.filter { it.name == "${targetName}Test" })
            }

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
                .also {
                    println("mergedConfig for ${it.name}")
                    it.fieldSpecs.forEach { key, value -> println("name: $key") }
                }
                .let { TargetConfigFile(targetName.platformType, outputDirectories[targetName]!!, it) }
        }

        val data = BuildKonfigData(
            packageName = packageName,
            commonConfig = TargetConfigFile(KotlinPlatformType.common.name, commonOutputDirectory, defaultConfig),
            targetConfigs = mergedConfigFiles
        )

        BuildKonfigEnvironment(data).generateConfigs { info -> logger.log(LogLevel.INFO, info) }

//        val config = targetConfigs
//            .filter {
//                it.name == "$targetName${compilationType!!.capitalize()}"
//                        || it.name == targetName
//            }
//            .sortedBy { it.name.length }
//            .fold(defaultConfigs) { previous, current -> mergeConfigs(previous, current) }
//
//        val data = BuildKonfigData(
//            packageName = packageName,
//            commonConfig = TargetConfigFile(commonOutputDirectory, defaultConfigs),
//            targetConfig = TargetConfigFile(outputDirectory, config)
//        )
//
//        BuildKonfigEnvironment(data).generateConfigs { info -> logger.log(LogLevel.INFO, info) }
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

    fun getTargetConfigFilterFun(targetName: String, compilationType: CompilationType): (TargetConfig) -> Boolean {
        return if (compilationType == CompilationType.MAIN) {
            { config: TargetConfig -> config.name == targetName || config.name == "${targetName}Main" }
        } else {
            { config: TargetConfig -> config.name == targetName || config.name == "${targetName}Main" || config.name == "${targetName}Test" }
        }
    }
}
