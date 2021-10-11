package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.VERSION
import com.codingfeline.buildkonfig.compiler.BuildKonfigData
import com.codingfeline.buildkonfig.compiler.BuildKonfigEnvironment
import com.codingfeline.buildkonfig.compiler.TargetConfig
import com.codingfeline.buildkonfig.compiler.TargetConfigFile
import com.codingfeline.buildkonfig.compiler.TargetName
import org.gradle.api.DefaultTask
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

    @get:Input
    lateinit var packageName: String

    @get:Input
    lateinit var objectName: String

    @Input
    var exposeObject: Boolean = false

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
    val targetOutputDirectories: Map<String, File>
        get() = outputDirectories.mapKeys { it.key.name }

    @get:OutputDirectory
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
        commonOutputDirectory.cleanupDirectory()
        targetOutputDirectories.forEach { it.value.cleanupDirectory() }

        val defaultConfig = mergeDefaultConfigs(logger::info, flavorName, defaultConfigs)

        val mergedConfigFiles = getMergedTargetConfigFiles(flavorName, defaultConfig)

        val data = BuildKonfigData(
            packageName = packageName,
            objectName = objectName,
            exposeObject = exposeObject,
            commonConfig = TargetConfigFile(
                TargetName("common", KotlinPlatformType.common.toPlatformType()),
                commonOutputDirectory,
                defaultConfig
            ),
            targetConfigs = mergedConfigFiles
        )

        BuildKonfigEnvironment(data).generateConfigs { info -> logger.info(info) }
    }

    private fun findFlavor(): String {
        val flavor = project.findProperty(FLAVOR_PROPERTY) ?: ""
        return if (flavor is String) {
            flavor
        } else {
            logger.error("$FLAVOR_PROPERTY must be string. Fallback to non-flavored config: ${flavor::class.java}")
            ""
        }
    }

    private fun getMergedTargetConfigFiles(
        flavorName: String,
        defaultConfig: TargetConfig
    ): List<TargetConfigFile> {
        val merged = mergeTargetConfigs(logger::info, flavorName, targetConfigs)
        return targetNames.map { targetName ->
            if (merged.isEmpty()) {
                return@map TargetConfigFile(targetName, outputDirectories.getValue(targetName), null)
            }

            val targetConfig = merged[targetName.name]
            TargetConfigFile(
                targetName = targetName,
                outputDirectory = outputDirectories.getValue(targetName),
                config = if (targetConfig != null) {
                    mergeConfigs(defaultConfig, checkTargetSpecificFields(defaultConfig, targetConfig))
                } else {
                    defaultConfig.copy()
                }
            )
        }
    }

    private fun File.cleanupDirectory() {
        deleteRecursively()
        mkdirs()
    }
}
