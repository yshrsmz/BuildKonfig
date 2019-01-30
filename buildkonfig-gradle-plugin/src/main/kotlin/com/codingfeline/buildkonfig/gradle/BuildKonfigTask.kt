package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.VERSION
import com.codingfeline.buildkonfig.compiler.PlatformConfig
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
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
    var isDebug: Boolean = false

    @Input
    var target: String? = null

    @Input
    fun getDefaultConfig(): PlatformConfig? {
        return extension.defaultConfigs?.toPlatformConfig()
    }

    @Input
    fun getTargetConfigs(): List<PlatformConfig> {
        return extension.targetConfigs?.map { it.toPlatformConfig() } ?: emptyList()
    }

    @OutputDirectory
    lateinit var outputDirectory: File

    private lateinit var extension: BuildKonfigExtension

    fun setExtension(extension: BuildKonfigExtension) {
        this.extension = extension
    }

    @TaskAction
    fun generateBuildKonfigFiles() {

        val defaultConfigs = getDefaultConfig()
        if (defaultConfigs != null) {
            logger.info("defaultConfig: ${defaultConfigs.name}")
            defaultConfigs.fieldSpecs.values.forEach { spec -> logger.info("spec: $spec.name, $spec.value") }
        }

        val targetConfigs = getTargetConfigs()
        if (targetConfigs.isEmpty()) {
            logger.info("configs is empty")
        } else {
            logger.info("configs.size(): ${targetConfigs.size}")
            targetConfigs.forEach { config ->
                logger.info("config: ${config.name}")
                config.fieldSpecs.values.forEach { spec -> logger.info("spec: $spec.name, $spec.value") }
            }
        }
    }
}
