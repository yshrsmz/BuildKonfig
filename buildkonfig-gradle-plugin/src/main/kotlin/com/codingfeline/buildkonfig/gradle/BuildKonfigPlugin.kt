package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.compiler.BuildKonfigLogger
import com.codingfeline.buildkonfig.compiler.LogLevel
import com.codingfeline.buildkonfig.compiler.PlatformType
import org.gradle.api.logging.Logger
import com.codingfeline.buildkonfig.compiler.TargetConfig
import com.codingfeline.buildkonfig.compiler.TargetName
import com.codingfeline.buildkonfig.gradle.kotlin.sources
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

typealias Flavor = String

const val DEFAULT_FLAVOR: Flavor = ""
const val COMMON_SOURCESET_NAME = "commonMain"
const val MAIN_SOURCESET_NAME = "main"

@Suppress("unused")
abstract class BuildKonfigPlugin : Plugin<Project> {

    private var configured = false

    override fun apply(target: Project) {
        val extension = target.extensions.create("buildkonfig", BuildKonfigExtension::class.java, target.logger)

        // Detect any supported Kotlin plugin (multiplatform / jvm / js / android).
        // KotlinBasePlugin is the common interface implemented by all Kotlin compiler
        // plugin wrappers, so this fires once for whichever Kotlin plugin the user applied.
        target.plugins.withType(KotlinBasePlugin::class.java) {
            if (configured) return@withType
            configured = true
            configure(target, extension)
        }

        target.afterEvaluate {
            check(configured) {
                "BuildKonfig Gradle plugin applied in project '${target.path}' " +
                    "but no supported Kotlin plugin was found. " +
                    "Apply one of: kotlin-multiplatform, kotlin-jvm, or kotlin-js."
            }
        }
    }

    private fun configure(project: Project, extension: BuildKonfigExtension) {
        // Register the task eagerly (outside afterEvaluate) so its outputs participate in
        // Gradle's task graph as Providers from the start. This is what allows downstream
        // tasks (e.g. KSP under Gradle 9.0) to discover an implicit dependency edge through
        // the source set's srcDirs Provider chain.
        val task = project.tasks.register("generateBuildKonfig", BuildKonfigTask::class.java) {
            it.group = "buildkonfig"
            it.description = "generate BuildKonfig"

            // Wire singleton DSL fields as Provider-to-Provider so the values are read lazily
            // at task execution time. Required inputs (e.g. packageName) surface as native
            // Gradle errors via @get:Input on the task if left unset.
            it.packageName.set(extension.packageName)
            it.objectName.set(
                extension.objectName.zip(extension.exposeObjectWithName.orElse("")) { obj, exposed ->
                    if (exposed.isNotBlank()) exposed else obj
                }
            )
            it.exposeObject.set(
                extension.exposeObjectWithName.map { it.isNotBlank() }.orElse(false)
            )
        }

        // The merge / source-set wiring depends on whether this is a multiplatform project
        // or a single-target Kotlin project. Both code paths run in afterEvaluate so the
        // user's DSL block and any KMP target registrations have already completed.
        project.afterEvaluate {
            val mppExtension = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
            if (mppExtension != null) {
                configureMultiplatform(project, extension, task, mppExtension)
            } else {
                configureSinglePlatform(project, extension, task)
            }
        }
    }

    private fun configureMultiplatform(
        project: Project,
        extension: BuildKonfigExtension,
        task: TaskProvider<BuildKonfigTask>,
        mppExtension: KotlinMultiplatformExtension,
    ) {
        val outputDirectory = project.layout.buildDirectory.dir("buildkonfig")

        val flavor = project.findFlavor()

        val mergedConfigs = extension.mergeConfigs(project.logger.toBuildKonfigLogger(), flavor)
            ?: return

        val targetConfigs = mergedConfigs.toMutableMap()

        val exposeObject = extension.exposeObjectWithName.getOrElse("").isNotBlank()

        // When both js and wasm targets exist with exposeObject, force expect/actual generation
        // so that @JsExport is only added to the JS actual (wasmJs doesn't support @JsExport on objects).
        val hasJsTarget = mppExtension.targets.any { t -> t.platformType == KotlinPlatformType.js }
        val hasWasmTarget = mppExtension.targets.any { t -> t.platformType == KotlinPlatformType.wasm }
        val forceExpectActual = exposeObject && hasJsTarget && hasWasmTarget

        val targetConfigSources =
            decideOutputs(project, mppExtension, targetConfigs, outputDirectory, forceExpectActual)

        task.configure { t ->
            t.flavor.set(flavor)
            t.hasJsTarget.set(hasJsTarget)
            t.commonSourceSetName.set(COMMON_SOURCESET_NAME)
            t.targetConfigFiles.set(targetConfigSources.mapValues { (_, value) -> value.configFile })
        }

        targetConfigSources.forEach { (key, configSource) ->
            val outputDirs = task.map { t -> listOfNotNull(t.outputDirectories[key]) }
            configSource.registerSourceDir(outputDirs)
        }
    }

    private fun configureSinglePlatform(
        project: Project,
        extension: BuildKonfigExtension,
        task: TaskProvider<BuildKonfigTask>,
    ) {
        val kotlinExtension = project.extensions.findByType(KotlinProjectExtension::class.java) ?: return

        val platformType = when (kotlinExtension) {
            is KotlinJvmProjectExtension -> KotlinPlatformType.jvm
            is KotlinJsProjectExtension -> KotlinPlatformType.js
            else -> {
                project.logger.warn(
                    "BuildKonfig: unsupported Kotlin extension '${kotlinExtension::class.java.simpleName}'. " +
                        "Skipping code generation."
                )
                return
            }
        }

        if (extension.targetConfigs.isNotEmpty()) {
            project.logger.warn(
                "BuildKonfig: targetConfigs are ignored in non-multiplatform projects (project '${project.path}'). " +
                    "Use defaultConfigs instead."
            )
        }

        val flavor = project.findFlavor()
        val mergedConfigs = extension.mergeConfigs(
            project.logger.toBuildKonfigLogger(),
            flavor,
            commonSourceSetName = MAIN_SOURCESET_NAME,
        ) ?: return

        // For non-KMP projects there is exactly one source set ("main"). Drop any
        // target-specific entries (the user was already warned) so the task generates a
        // single concrete object rather than an expect/actual pair.
        val mainConfig = mergedConfigs.getValue(MAIN_SOURCESET_NAME)
        val outputDirectory = project.layout.buildDirectory.dir("buildkonfig")
        val targetConfigFile = TargetConfigFileImpl(
            targetName = TargetName(MAIN_SOURCESET_NAME, platformType.toPlatformType()),
            outputDirectory = outputDirectory.map { it.dir(MAIN_SOURCESET_NAME) }.get().asFile,
            config = mainConfig,
        )

        task.configure { t ->
            t.flavor.set(flavor)
            t.hasJsTarget.set(platformType == KotlinPlatformType.js)
            t.commonSourceSetName.set(MAIN_SOURCESET_NAME)
            t.targetConfigFiles.set(mapOf(MAIN_SOURCESET_NAME to targetConfigFile))
        }

        val mainSourceSet = kotlinExtension.sourceSets.getByName(MAIN_SOURCESET_NAME)
        val outputDirs = task.map { t -> listOfNotNull(t.outputDirectories[MAIN_SOURCESET_NAME]) }
        mainSourceSet.kotlin.srcDirs(outputDirs)
    }
}

fun decideOutputs(
    project: Project,
    mppExtension: KotlinMultiplatformExtension,
    targetConfigs: MutableMap<String, TargetConfig>,
    outputDirectory: Provider<Directory>,
    forceExpectActual: Boolean = false
): Map<String, TargetConfigSource> {
    return mppExtension.sources()
        // Map<SourceName, TargetConfigSource>
        .fold(emptyMap()) { acc, source ->
            if (targetConfigs.size == 1 && source.name != COMMON_SOURCESET_NAME && !forceExpectActual) {
                // there's only common config
                return@fold acc
            }

            val dependentsWithConfig = source.sourceSets
                .filter { it.name != COMMON_SOURCESET_NAME }
                .filter { targetConfigs.containsKey(it.name) }

            val sourceHasConfig = targetConfigs.containsKey(source.name)

            if (dependentsWithConfig.isNotEmpty()) {
                if (sourceHasConfig) {
                    project.logger.warn(
                        "BuildKonfig configuration for SourceSet(${source.name}) is ignored, " +
                                "as its dependent SourceSets(${dependentsWithConfig.map { it.name }}) also have configurations"
                    )
                }

                val firstDependent = dependentsWithConfig.first()
                if (acc.containsKey(firstDependent.name)) {
                    // common source set should be available earlier, as sources are sorted by the number of dependent SourceSets
                    return@fold acc
                }

                // if not available, create it.
                val tcs = TargetConfigSource(
                    name = firstDependent.name,
                    configFile = TargetConfigFileImpl(
                        targetName = TargetName(firstDependent.name, source.type.toPlatformType()),
                        outputDirectory = outputDirectory.map { it.dir(firstDependent.name) }.get().asFile,
                        config = targetConfigs.getValue(firstDependent.name)
                    ),
                    registerSourceDir = { dir -> firstDependent.kotlin.srcDirs(dir) }
                )

                return@fold acc + (firstDependent.name to tcs)
            }

            if (source.type == KotlinPlatformType.common && !sourceHasConfig) {
                // Intermediate SourceSets without config
                return@fold acc
            }

            val targetSourceSet = if (sourceHasConfig || dependentsWithConfig.isEmpty()) {
                source.defaultSourceSet
            } else {
                dependentsWithConfig.first()
            }
            val tcs = TargetConfigSource(
                name = source.name,
                configFile = TargetConfigFileImpl(
                    targetName = TargetName(source.name, source.type.toPlatformType()),
                    outputDirectory = outputDirectory.map { it.dir(targetSourceSet.name) }.get().asFile,
                    config = targetConfigs[source.name] ?: targetConfigs.getValue(COMMON_SOURCESET_NAME).copy(),
                ),
                registerSourceDir = { dir -> targetSourceSet.kotlin.srcDirs(dir) }
            )

            acc + (source.name to tcs)
        }
}

/**
 * Resolves the BuildKonfig flavor from project properties.
 *
 * Uses [Project.findProperty] (rather than [org.gradle.api.provider.ProviderFactory.gradleProperty])
 * so that callers can influence the flavor from the build script — e.g. by inspecting
 * `gradle.startParameter.taskNames` and calling `project.extensions.extraProperties.set(...)` —
 * before any `afterEvaluate` block runs. Resolution happens once during configuration and the
 * result is captured as a String constant on the task input, so configuration cache is unaffected.
 */
internal fun Project.findFlavor(): String {
    val flavor = findProperty(FLAVOR_PROPERTY) ?: ""
    return if (flavor is String) {
        flavor
    } else {
        logger.error("$FLAVOR_PROPERTY must be string. Fallback to non-flavored config: ${flavor::class.java}")
        DEFAULT_FLAVOR
    }
}

internal fun Logger.toBuildKonfigLogger(): BuildKonfigLogger {
    return BuildKonfigLogger { level, message ->
        when (level) {
            LogLevel.INFO -> info(message)
            LogLevel.WARN -> warn(message)
        }
    }
}

internal fun KotlinPlatformType.toPlatformType(): PlatformType {
    return when (this) {
        KotlinPlatformType.common -> PlatformType.common
        KotlinPlatformType.jvm -> PlatformType.jvm
        KotlinPlatformType.js -> PlatformType.js
        KotlinPlatformType.androidJvm -> PlatformType.androidJvm
        KotlinPlatformType.native -> PlatformType.native
        KotlinPlatformType.wasm -> PlatformType.wasm
    }
}
