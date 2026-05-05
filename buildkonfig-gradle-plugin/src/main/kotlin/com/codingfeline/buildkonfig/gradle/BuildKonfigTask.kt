package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.VERSION
import com.codingfeline.buildkonfig.compiler.BuildKonfigData
import com.codingfeline.buildkonfig.compiler.BuildKonfigEnvironment
import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

const val FLAVOR_PROPERTY = "buildkonfig.flavor"

@DisableCachingByDefault(because = "BuildKonfig generation is fast enough that caching adds little value")
abstract class BuildKonfigTask : DefaultTask() {

    // Required to invalidate the task on version updates.
    @Suppress("unused")
    @get:Input
    val pluginVersion: String
        get() = VERSION

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val objectName: Property<String>

    @get:Input
    abstract val exposeObject: Property<Boolean>

    @get:Input
    abstract val hasJsTarget: Property<Boolean>

    @get:Input
    abstract val flavor: Property<String>

    /**
     * Name of the source set whose merged config is treated as the "common" config.
     * - KMP projects: `commonMain` (the default).
     * - Non-KMP projects: typically `main`, since there is no expect/actual split.
     */
    @get:Input
    abstract val commonSourceSetName: Property<String>

    @get:Nested
    abstract val targetConfigFiles: MapProperty<String, TargetConfigFileImpl>

    @Suppress("unused")
    @get:OutputDirectories
    val outputDirectories: Map<String, File>
        get() = targetConfigFiles.get().mapValues { it.value.outputDirectory }

    @Suppress("unused")
    @TaskAction
    fun generateBuildKonfigFiles() {
        val commonName = commonSourceSetName.get()
        val outputDirs = outputDirectories
        // clean up output directories
        outputDirs.getValue(commonName).parentFile.cleanupDirectory()
        outputDirs.forEach { it.value.mkdirs() }

        val configFiles = targetConfigFiles.get()
        val data = BuildKonfigData(
            packageName = packageName.get(),
            objectName = objectName.get(),
            exposeObject = exposeObject.get(),
            commonConfig = configFiles.getValue(commonName),
            targetConfigs = configFiles.filter { it.key != commonName }.values.toList(),
            hasJsTarget = hasJsTarget.get()
        )

        BuildKonfigEnvironment(data).generateConfigs(logger.toBuildKonfigLogger())
    }

    private fun File.cleanupDirectory() {
        deleteRecursively()
        mkdirs()
    }
}
