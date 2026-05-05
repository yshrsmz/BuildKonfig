package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.VERSION
import com.codingfeline.buildkonfig.compiler.BuildKonfigData
import com.codingfeline.buildkonfig.compiler.BuildKonfigEnvironment
import com.codingfeline.buildkonfig.compiler.TargetConfig
import com.codingfeline.buildkonfig.compiler.TargetConfigFile
import com.codingfeline.buildkonfig.compiler.TargetName
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

const val FLAVOR_PROPERTY = "buildkonfig.flavor"

@CacheableTask
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
    abstract val targetConfigFiles: MapProperty<String, TargetConfigInput>

    /**
     * Root directory containing all generated BuildKonfig sources, with one subdirectory
     * per source set (e.g. `build/buildkonfig/commonMain`, `build/buildkonfig/jvmMain`).
     * Declared as a single `@OutputDirectory` so the entire subtree participates in the
     * cache key / restore cycle, and stale subdirectories from removed source sets
     * cannot leak through.
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @Suppress("unused")
    @TaskAction
    fun generateBuildKonfigFiles() {
        // Gradle does not auto-clean `@OutputDirectory` content for ad-hoc tasks, so we
        // wipe the root explicitly. On cache hits the action does not run and Gradle
        // performs the cleanup + restore itself.
        val outputRoot = outputDirectory.get().asFile
        outputRoot.deleteRecursively()

        val commonName = commonSourceSetName.get()
        val resolvedConfigs = targetConfigFiles.get().mapValues { (name, input) ->
            ResolvedTargetConfigFile(
                targetName = input.targetName,
                outputDirectory = outputRoot.resolve(name),
                config = input.config,
            )
        }
        val data = BuildKonfigData(
            packageName = packageName.get(),
            objectName = objectName.get(),
            exposeObject = exposeObject.get(),
            commonConfig = resolvedConfigs.getValue(commonName),
            targetConfigs = resolvedConfigs.filter { it.key != commonName }.values.toList(),
            hasJsTarget = hasJsTarget.get()
        )

        BuildKonfigEnvironment(data).generateConfigs(logger.toBuildKonfigLogger())
    }
}

/**
 * Compiler-facing [TargetConfigFile] built fresh for each task action by combining a
 * [TargetConfigInput] with a sub-path of `BuildKonfigTask.outputDirectory`. Kept private
 * to the task: it never participates in cache-key snapshotting or configuration cache
 * serialization (those are handled by `TargetConfigInput`).
 */
private data class ResolvedTargetConfigFile(
    override val targetName: TargetName,
    override val outputDirectory: File,
    override val config: TargetConfig?,
) : TargetConfigFile
