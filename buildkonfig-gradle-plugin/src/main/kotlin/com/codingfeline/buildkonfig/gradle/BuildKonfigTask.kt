package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.VERSION
import com.codingfeline.buildkonfig.compiler.BuildKonfigData
import com.codingfeline.buildkonfig.compiler.BuildKonfigEnvironment
import com.codingfeline.buildkonfig.compiler.TargetConfig
import com.codingfeline.buildkonfig.compiler.TargetConfigFile
import com.codingfeline.buildkonfig.compiler.TargetName
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectories
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
     * Root directory beneath which per-source-set outputs live. Declared `@Internal`
     * (not an output) because AGP 9+ `prepareAndroidMainArtProfile`
     * (`ProcessLibraryArtProfileTask`) probes `<root>/baselineProfiles/baseline-prof.txt`
     * for each generated source root it inherits from the Kotlin source set. If the
     * root itself were an `@OutputDirectory`, Gradle's strict input/output overlap
     * validation would reject the build with an "implicit dependency" error from
     * `prepareAndroidMainArtProfile` to this task. The actual outputs are tracked via
     * [outputDirectories], which list only the per-source-set leaves.
     */
    @get:Internal
    abstract val outputDirectory: DirectoryProperty

    /**
     * Per-source-set output directories — one entry per source set the merged config
     * resolves to (e.g. `build/generated/source/buildkonfig/commonMain`,
     * `.../jvmMain`). Each leaf matches the Kotlin source set's registered `srcDir`,
     * and is the cache-key + task-dependency surface for downstream consumers
     * (KSP, baseline profiles, ...).
     */
    @get:OutputDirectories
    abstract val outputDirectories: MapProperty<String, Directory>

    @Suppress("unused")
    @TaskAction
    fun generateBuildKonfigFiles() {
        // Wipe the entire root so subdirectories from source sets that no longer appear in
        // [outputDirectories] (e.g. a target the user just removed) don't linger. The root
        // itself is `@Internal` — only the per-source-set leaves below participate in
        // Gradle's cache key / restore cycle — so this cleanup must happen explicitly here.
        //
        // Limitation: on a build-cache hit this action does not run; Gradle restores only
        // the declared per-leaf `@OutputDirectories`. Orphan subdirectories left over from
        // a previous non-cached run for a since-removed source set will persist on disk in
        // that case. `./gradlew clean` clears them.
        val outputRoot = outputDirectory.get().asFile
        outputRoot.deleteRecursively()

        val outputs = outputDirectories.get()
        val commonName = commonSourceSetName.get()
        val resolvedConfigs = targetConfigFiles.get().mapValues { (name, input) ->
            ResolvedTargetConfigFile(
                targetName = input.targetName,
                outputDirectory = outputs.getValue(name).asFile,
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
