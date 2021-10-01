package com.codingfeline.buildkonfig.gradle


import com.codingfeline.buildkonfig.compiler.PlatformType
import com.codingfeline.buildkonfig.compiler.TargetName
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import java.io.File

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
        val commonOutputDirectory = File(outputDirectory, "commonMain").also { it.mkdirs() }

        val mppExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val targets = mppExtension.targets

        val outputDirectoryMap = mutableMapOf<TargetName, File>()

        targets.filter { it !is KotlinMetadataTarget }.forEach { target ->
            val name = "${target.name}Main"
            val outDirMain = File(outputDirectory, name).also { it.mkdirs() }
            outputDirectoryMap[TargetName(target.name, target.platformType.toPlatformType())] = outDirMain
        }

        project.afterEvaluate { p ->

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
                it.commonOutputDirectory = commonOutputDirectory
                it.outputDirectories = outputDirectoryMap
                it.extension = extension

                it.group = "buildkonfig"
                it.description = "generate BuildKonfig"
            }

            targets.forEach { target ->
                target.compilations
                    .filter { !it.name.endsWith(suffix = "Test", ignoreCase = true) }
                    .forEach eachCompilation@{ compilation ->
                        if (target is KotlinMetadataTarget && compilation.defaultSourceSet.dependsOn.isNotEmpty()) {
                            // When `kotlin.mpp.enableGranularSourceSetsMetadata` is set to true,
                            // shared SourceSet have its dedicated compilation in KotlinMetadataTarget
                            // So we check if its defaultSourceSet has any dependency to check if it's commonMain
                            // https://github.com/yshrsmz/BuildKonfig/issues/56
                            return@eachCompilation
                        }
                        val outputDirs = task.map { t ->
                            val src = if (target is KotlinMetadataTarget) {
                                t.commonOutputDirectory
                            } else {
                                t.targetOutputDirectories[target.name]
                            }
                            listOfNotNull(src)
                        }
                        compilation.defaultSourceSet.kotlin.srcDirs(outputDirs)
                    }
            }
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
    }
}
