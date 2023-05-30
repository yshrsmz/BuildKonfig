package com.codingfeline.buildkonfig.gradle


import com.codingfeline.buildkonfig.compiler.PlatformType
import com.codingfeline.buildkonfig.compiler.TargetConfig
import com.codingfeline.buildkonfig.compiler.TargetName
import com.codingfeline.buildkonfig.gradle.kotlin.sources
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.targets
import java.io.File

typealias Flavor = String

const val DEFAULT_FLAVOR: Flavor = ""
const val COMMON_SOURCESET_NAME = "commonMain"

@Suppress("unused")
abstract class BuildKonfigPlugin : Plugin<Project> {

    override fun apply(target: Project) {

        var isSupported = false
        target.plugins.all { plugin ->
            if (plugin is KotlinBasePluginWrapper) when (plugin.projectExtensionClass) {
                KotlinMultiplatformExtension::class,
                KotlinAndroidProjectExtension::class,
                KotlinJvmProjectExtension::class,
                -> {
                    isSupported = true
                }
            }
        }

        val extension = target.extensions.create("buildkonfig", BuildKonfigExtension::class.java, target)

        target.afterEvaluate {
            if (!isSupported) error(
                """
                BuildKonfig Gradle plugin applied in project '${target.path}' but no supported Kotlin plugin was found.

                Only the following plugins are currently supported at the moment:
                - `kotlin("multiplatform")` or `id("org.jetbrains.kotlin.multiplatform")`
                - `kotlin("android")` or `id("org.jetbrains.kotlin.android")`
                - `kotlin("jvm")` or `id("org.jetbrains.kotlin.jvm")`
                """.trimIndent()
            )

            configure(target, extension)
        }
    }

    private fun configure(project: Project, extension: BuildKonfigExtension) {
        val outputDirectory = File(project.buildDir, "buildkonfig")

        val kotlinExtension = project.extensions.getByType(KotlinProjectExtension::class.java)

        project.afterEvaluate(fun(project: Project) {
            val flavor = project.findFlavor()

            val targetConfigs = extension.mergeConfigs(project.logger::info, flavor)
                .toMutableMap()

            val targetConfigSources = decideOutputs(project, kotlinExtension, targetConfigs, outputDirectory)

            val task = project.tasks.register("generateBuildKonfig", BuildKonfigTask::class.java) {
                it.packageName = requireNotNull(extension.packageName) { "packageName must be provided" }
                require(extension.objectName.isNotBlank()) { "objectName must not be blank" }

                var objectName = extension.objectName
                var exposeObject = false
                extension.exposeObjectWithName.takeIf { name -> !name.isNullOrBlank() }
                    ?.also { name ->
                        objectName = name
                        exposeObject = true
                    }

                it.objectName = objectName
                it.exposeObject = exposeObject
                it.hasJsTarget = kotlinExtension.targets.any { t -> t.platformType == KotlinPlatformType.js }
                it.flavor = flavor
                it.targetConfigFiles = targetConfigSources.mapValues { (_, value) -> value.configFile }

                it.group = "buildkonfig"
                it.description = "generate BuildKonfig"
            }

            targetConfigSources.forEach { (key, configSource) ->
                val outputDirs = task.map { t -> listOfNotNull(t.outputDirectories[key]) }
                configSource.sourceSet.kotlin.srcDirs(outputDirs)
            }
        })
    }
}

fun decideOutputs(
    project: Project,
    kotlinExtension: KotlinProjectExtension,
    targetConfigs: MutableMap<String, TargetConfig>,
    outputDirectory: File
): Map<String, TargetConfigSource> {
    val acc = linkedMapOf<String, TargetConfigSource>()
    for (source in kotlinExtension.sources()) {
        if (targetConfigs.size == 1 && source.name != COMMON_SOURCESET_NAME) {
            // there's only common config
            continue
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
                continue
            }

            // if not available, create it.
            val tcs = TargetConfigSource(
                name = firstDependent.name,
                configFile = TargetConfigFileImpl(
                    targetName = TargetName(firstDependent.name, source.type.toPlatformType()),
                    outputDirectory = File(outputDirectory, firstDependent.name),
                    config = targetConfigs.getValue(firstDependent.name)
                ),
                sourceSet = firstDependent
            )

            acc[firstDependent.name] = tcs
            continue
        }

        if (source.type == KotlinPlatformType.common && !sourceHasConfig) {
            // Intermediate SourceSets without config
            continue
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
                outputDirectory = File(outputDirectory, targetSourceSet.name),
                config = targetConfigs[source.name] ?: targetConfigs.getValue(COMMON_SOURCESET_NAME).copy(),
            ),
            sourceSet = targetSourceSet
        )

        acc[source.name] = tcs
    }
    return acc
}

internal fun Project.findFlavor(): String {
    val flavor = findProperty(FLAVOR_PROPERTY) ?: ""
    return if (flavor is String) {
        flavor
    } else {
        logger.error("$FLAVOR_PROPERTY must be string. Fallback to non-flavored config: ${flavor::class.java}")
        DEFAULT_FLAVOR
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


