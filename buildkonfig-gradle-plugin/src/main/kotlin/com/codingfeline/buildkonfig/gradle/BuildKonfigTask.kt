package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.VERSION
import com.codingfeline.buildkonfig.compiler.BuildKonfigData
import com.codingfeline.buildkonfig.compiler.TargetConfig
import com.codingfeline.buildkonfig.compiler.TargetConfigFile
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.File

open class BuildKonfigTask : DefaultTask() {

    // Required to invalidate the task on version updates.
    @Input
    fun pluginVersion(): String {
        return VERSION
    }

    @Input
    var targetName: String? = null

    // main or test
    @Input
    var compilationType: String? = null

    @Input
    var platformType: KotlinPlatformType? = null

    @Input
    fun getPackageName(): String? {
        return extension.packageName
    }

    @Input
    fun getDefaultConfig(): TargetConfig? {
        return extension.defaultConfigs?.toPlatformConfig()
    }

    @Input
    fun getTargetConfigs(): List<TargetConfig> {
        return extension.targetConfigs?.map { it.toPlatformConfig() } ?: emptyList()
    }

    @OutputDirectory
    lateinit var commonOutputDirectory: File

    @OutputDirectory
    lateinit var outputDirectory: File

    private lateinit var extension: BuildKonfigExtension

    fun setExtension(extension: BuildKonfigExtension) {
        this.extension = extension
    }

    @TaskAction
    fun generateBuildKonfigFiles() {

        val defaultConfigs = getDefaultConfig() ?: throw IllegalStateException("defaultConfigs must be provided")
        val packageName = getPackageName() ?: throw java.lang.IllegalStateException("packageName must be provided")

        val targetConfigs = getTargetConfigs()
        val config = targetConfigs
            .filter {
                it.name == "$targetName${compilationType!!.capitalize()}"
                        || it.name == targetName
            }
            .sortedBy { it.name.length }
            .fold(defaultConfigs) { previous, current -> mergeConfigs(previous, current) }

        val data = BuildKonfigData(
            packageName = packageName,
            commonConfig = TargetConfigFile(commonOutputDirectory, defaultConfigs),
            targetConfig = TargetConfigFile(outputDirectory, config)
        )

    }

    fun mergeConfigs(baseConfig: TargetConfig, newConfig: TargetConfig): TargetConfig {
        val result = TargetConfig(targetName!!)

        baseConfig.fieldSpecs.forEach { name, value ->
            result.fieldSpecs[name] = value.copy()
        }

        newConfig.fieldSpecs.forEach { name, value ->
            val alreadyPresent = result.fieldSpecs[name]
            if (alreadyPresent != null) {
                logger.info("BuildKonfig: buildConfigField '$name' is being replaced: ${alreadyPresent.value} -> ${value.value}")
            }
            result.fieldSpecs[name] = value.copy()
        }


        return result
    }
}
