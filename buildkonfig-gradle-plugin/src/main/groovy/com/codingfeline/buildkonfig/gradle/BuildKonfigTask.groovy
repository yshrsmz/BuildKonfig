package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.PlatformConfig
import com.codingfeline.buildkonfig.VersionKt
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class BuildKonfigTask extends DefaultTask {

    // Required to invalidate the task on version updates.
    @Input
    String pluginVersion() {
        return VersionKt.getVERSION()
    }

    @Input
    PlatformConfig getDefaultConfig() {
        return extension.defaultConfigs.toPlatformConfig()
    }

    @Input
    List<PlatformConfig> getTargetConfigs() {
        return extension.targetConfigs.collect { it.toPlatformConfig() }
    }

    @OutputDirectory
    File outputDirectory = null

    private BuildKonfigExtension extension

    void setExtension(BuildKonfigExtension extension) {
        this.extension = extension
    }

    @TaskAction
    void generateBuildKonfigFiles() {

        def defaultConfigs = getDefaultConfig()
        if (defaultConfigs != null) {
            logger.info("defaultConfig: ${defaultConfigs.name}")
            defaultConfigs.fieldSpecs.values().forEach { spec -> logger.info("spec: $spec.name, $spec.value") }
        }

        def targetConfigs = getTargetConfigs()
        if (targetConfigs == null) {
            logger.info("configs is null")
        } else {
            logger.info("configs.size(): ${targetConfigs.size()}")
            targetConfigs.forEach { config ->
                logger.info("config: ${config.name}")
                config.fieldSpecs.values().forEach { spec -> logger.info("spec: $spec.name, $spec.value") }
            }
        }
    }
}
