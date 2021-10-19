package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.VERSION
import com.codingfeline.buildkonfig.compiler.BuildKonfigData
import com.codingfeline.buildkonfig.compiler.BuildKonfigEnvironment
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.TaskAction
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

    @Input
    var hasJsTarget: Boolean = false

    @get:Input
    lateinit var flavor: String

    @get:Nested
    lateinit var targetConfigFiles: Map<String, TargetConfigFileImpl>

    @Internal
    lateinit var extension: BuildKonfigExtension

    @Suppress("unused")
    @get:OutputDirectories
    val outputDirectories: Map<String, File>
        get() = targetConfigFiles.mapValues { it.value.outputDirectory }

    @Suppress("unused")
    @TaskAction
    fun generateBuildKonfigFiles() {
        // clean up output directories
        outputDirectories.getValue(COMMON_SOURCESET_NAME).parentFile.cleanupDirectory()
        outputDirectories.forEach { it.value.mkdirs() }

        val data = BuildKonfigData(
            packageName = packageName,
            objectName = objectName,
            exposeObject = exposeObject,
            commonConfig = targetConfigFiles.getValue(COMMON_SOURCESET_NAME),
            targetConfigs = targetConfigFiles.filter { it.key !== COMMON_SOURCESET_NAME }.values.toList(),
            hasJsTarget = hasJsTarget
        )

        BuildKonfigEnvironment(data).generateConfigs { info -> logger.info(info) }
    }

    private fun File.cleanupDirectory() {
        deleteRecursively()
        mkdirs()
    }
}
