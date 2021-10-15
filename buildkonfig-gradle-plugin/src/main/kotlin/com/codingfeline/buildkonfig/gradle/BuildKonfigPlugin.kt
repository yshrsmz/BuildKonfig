package com.codingfeline.buildkonfig.gradle


import com.codingfeline.buildkonfig.compiler.PlatformType
import com.codingfeline.buildkonfig.compiler.TargetConfig
import com.codingfeline.buildkonfig.compiler.TargetName
import com.codingfeline.buildkonfig.gradle.kotlin.sources
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import java.io.File

typealias Flavor = String

const val DEFAULT_FLAVOR: Flavor = ""

@Suppress("unused")
abstract class BuildKonfigPlugin : Plugin<Project> {

    override fun apply(target: Project) {

        var isMultiplatform = false
        target.plugins.all { p ->
            if (p is KotlinMultiplatformPluginWrapper) {
                isMultiplatform = true
            }
        }

        val extension = target.extensions.create("buildkonfig", BuildKonfigExtension::class.java, target)

        target.afterEvaluate {
            if (!isMultiplatform) {
                throw IllegalStateException(
                    "BuildKonfig Gradle plugin applied in project '${target.path}' " +
                            "but no supported Kotlin multiplatform plugin was found"
                )
            }

            configure(target, extension)
        }
    }

    private fun configure(project: Project, extension: BuildKonfigExtension) {
        val outputDirectory = File(project.buildDir, "buildkonfig")

        val mppExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

        project.afterEvaluate { p ->
            val flavor = p.findFlavor()

            val targetConfigs = extension.mergeConfigs(project.logger::info, flavor)
                .mapKeys { (key, _) -> "${key}Main" }
                .toMutableMap()

            println("targetConfigs: $targetConfigs")

            val targetConfigSources = mppExtension.sources()
                .mapNotNull { source ->
                    if (targetConfigs.size == 1 && source.name != targetConfigs.keys.first()) {
                        return@mapNotNull null
                    }

                    val dependentsWithConfig = source.sourceSets.filter { it.name != "commonMain" }
                        .filter { targetConfigs.containsKey(it.name) }
                    val sourceHasConfig = targetConfigs.containsKey(source.name)

                    println("------")
                    println("source: ${source.name}, ${source.sourceSets}")
                    println("dependentWithConfig: $dependentsWithConfig")
                    println("sourceHasConfig: ${targetConfigs[source.name]}")
                    println("------")
                    if (sourceHasConfig) {
                        if (dependentsWithConfig.isNotEmpty()) {
                            project.logger.warn(
                                "BuildKonfig configuration for SourceSet(${source.name}) is ignored, " +
                                        "as its dependent SourceSets(${dependentsWithConfig.map { it.name }}) also have configurations"
                            )
                            targetConfigs.remove(source.name)
                            return@mapNotNull null
                        }
                    }
                    if (source.type == KotlinPlatformType.common && !sourceHasConfig) {
                        return@mapNotNull null
                    }

                    val targetSourceSet = if (sourceHasConfig || dependentsWithConfig.isEmpty()) {
                        source.defaultSourceSet
                    } else {
                        dependentsWithConfig.first()
                    }
                    TargetConfigSource(
                        configFile = TargetConfigFileImpl(
                            targetName = TargetName(source.name, source.type.toPlatformType()),
                            outputDirectory = File(outputDirectory, targetSourceSet.name),
                            config = targetConfigs[source.name] ?: targetConfigs.getValue("commonMain").copy(),
                        ),
                        sourceSet = targetSourceSet,
                        source = source
                    ).also {
                        println("result TargetConfigSource: $it")
                    }
                }

            val outputDirectories = targetConfigSources.associate { s ->
                s.sourceSet.name to s.configFile.outputDirectory
            }

            println("outputdirectories: $outputDirectories")

            val task = p.tasks.register("generateBuildKonfig", BuildKonfigTask::class.java) {
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
                it.hasJsTarget = mppExtension.targets.any { t -> t.platformType == KotlinPlatformType.js }
                it.flavor = flavor
                it.targetConfigFiles = targetConfigSources.associate { s -> s.sourceSet.name to s.configFile }
                it.extension = extension

                it.group = "buildkonfig"
                it.description = "generate BuildKonfig"
            }

            targetConfigSources.forEach { configSource ->
                val outputDirs = task.map { t -> listOfNotNull(t.outputDirectories[configSource.source.name]) }
                configSource.sourceSet.kotlin.srcDirs(outputDirs)
            }


//            targets.forEach { target ->
//                println("target: ${target.name}, ${target::class}")
//                target.compilations
//                    .filter { !it.name.endsWith(suffix = "Test", ignoreCase = true) }
//                    .forEach eachCompilation@{ compilation ->
//                        println("compilation: ${compilation.name}, ${compilation::class}, ${compilation.compileKotlinTask}, ${compilation.compileAllTaskName}")
//                        println("compilation: defaultSourceSet: ${compilation.defaultSourceSet}, dependsOn: ${compilation.defaultSourceSet.dependsOn}")
//                        println("compilation: allKotlinSourceSets: ${compilation.allKotlinSourceSets}")
//                        println("------")
//                        if (target is KotlinMetadataTarget && compilation.defaultSourceSet.dependsOn.isNotEmpty()) {
//                            // When `kotlin.mpp.enableGranularSourceSetsMetadata` is set to true,
//                            // shared SourceSet have its dedicated compilation in KotlinMetadataTarget
//                            // So we check if its defaultSourceSet has any dependency to check if it's commonMain
//                            // https://github.com/yshrsmz/BuildKonfig/issues/56
//                            return@eachCompilation
//                        }
//                        val outputDirs = task.map { t ->
//                            val src = if (target is KotlinMetadataTarget) {
//                                t.outputDirectories["commonMain"]
//                            } else {
//                                t.outputDirectories["${target.name}Main"]
//                            }
//                            listOfNotNull(src)
//                        }
//                        compilation.defaultSourceSet.kotlin.srcDirs(outputDirs)
//                    }
//            }
        }
    }

    private fun getFiles(
        targets: NamedDomainObjectCollection<KotlinTarget>,
        targetConfigs: MutableMap<String, TargetConfig>,
        outputDirectory: File
    ): List<TargetConfigFileImpl> {
        val files = targets
            .flatMap { target ->
                target.compilations.filter { !it.name.endsWith(suffix = "Test", ignoreCase = true) }
                    .mapNotNull { compilation ->

                        val defaultSourceSet = compilation.defaultSourceSet

                        val allSourceSets = compilation.allKotlinSourceSets
                            .filter { it.name != "commonMain" && it.name != defaultSourceSet.name }
                        val allSourceSetsWithConfig = allSourceSets
                            .filter { targetConfigs.containsKey(it.name) }

                        val defaultSourceSetHasConfig =
                            targetConfigs.containsKey(defaultSourceSet.name)

                        if (allSourceSetsWithConfig.isNotEmpty() && defaultSourceSetHasConfig) {
                            // both dependentSourceSets and defaultSourceSet has targetConfig,
                            // and that is not compilable
                            throw IllegalStateException(
                                "SourceSet['${defaultSourceSet.name}'] has BuildKonfig configuration, " +
                                        "but its dependent SourceSets[${allSourceSetsWithConfig}] also have BuildKonfig configuration."
                            )
                        }

                        if (!defaultSourceSetHasConfig && allSourceSetsWithConfig.isNotEmpty()) {
                            targetConfigs[defaultSourceSet.name] =
                                targetConfigs.getValue("commonMain").copy()
                        }

                        if (allSourceSetsWithConfig.isNotEmpty()) {
                            // BuildKonfig should be generated by the dependent SourceSet
                            null
                        } else {
                            TargetConfigFileImpl(
                                targetName = TargetName(
                                    name = defaultSourceSet.name,
                                    platformType = target.platformType.toPlatformType()
                                ),
                                outputDirectory = File(outputDirectory, defaultSourceSet.name),
                                config = targetConfigs[defaultSourceSet.name]
                            )
                        }
                    }
            }
        return files
    }
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
    }
}


