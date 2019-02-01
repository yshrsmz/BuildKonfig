package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.VERSION
import com.codingfeline.buildkonfig.compiler.CompilationType
import com.codingfeline.buildkonfig.compiler.TargetConfig
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class BuildKonfigTask : DefaultTask() {

    // Required to invalidate the task on version updates.
    @Input
    fun pluginVersion(): String {
        return VERSION
    }

    @Input
    lateinit var compilationType: CompilationType

    @Input
    lateinit var packageName: String

    @Input
    lateinit var targetNames: List<String>

    lateinit var extension: BuildKonfigExtension


    @Input
    fun getDefaultConfig(): TargetConfig? {
        return extension.defaultConfigs?.toPlatformConfig()
    }

    @Input
    fun getTargetConfigs(): List<TargetConfig> {
        return extension.targetConfigs?.map { it.toPlatformConfig() } ?: emptyList()
    }

    @OutputDirectories
    fun targetOutputDirectories(): List<File> {
        return outputDirectories.values.toList()
    }

    @OutputDirectory
    lateinit var commonOutputDirectory: File

    lateinit var outputDirectories: Map<String, File>

    @TaskAction
    fun generateBuildKonfigFiles() {

        val defaultConfigs = getDefaultConfig() ?: throw IllegalStateException("defaultConfigs must be provided")

        val targetConfigs = getTargetConfigs()

        val mergedConfigs = targetNames.map { targetName ->
            println("merging configs for $targetName")
            val sortedConfigs = mutableListOf<TargetConfig>()
            sortedConfigs.addAll(targetConfigs.filter { it.name == targetName })
            sortedConfigs.addAll(targetConfigs.filter { it.name == "${targetName}Main" })

            if (compilationType == CompilationType.TEST) {
                sortedConfigs.addAll(targetConfigs.filter { it.name == "${targetName}Test" })
            }

            val defaultConfigsForTarget = TargetConfig(targetName)
                .apply {
                    this.fieldSpecs.putAll(defaultConfigs.fieldSpecs)
                }

            sortedConfigs
                .fold(defaultConfigsForTarget) { previous, current -> mergeConfigs(targetName, previous, current) }
                .also {
                    println("mergedConfig for ${it.name}")
                    it.fieldSpecs.forEach { key, value -> println("name: $key") }
                }
        }

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

    fun mergeConfigs(targetName: String, baseConfig: TargetConfig, newConfig: TargetConfig): TargetConfig {
        val result = TargetConfig(targetName)

        baseConfig.fieldSpecs.forEach { name, value ->
            result.fieldSpecs[name] = value.copy()
        }

        newConfig.fieldSpecs.forEach { name, value ->
            val alreadyPresent = result.fieldSpecs[name]
            if (alreadyPresent != null) {
                logger.info("BuildKonfig for $targetName: buildConfigField '$name' is being replaced: ${alreadyPresent.value} -> ${value.value}")
            }
            result.fieldSpecs[name] = value.copy()
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
