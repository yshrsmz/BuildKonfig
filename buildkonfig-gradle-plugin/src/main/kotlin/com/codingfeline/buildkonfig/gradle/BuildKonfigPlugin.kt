package com.codingfeline.buildkonfig.gradle


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import java.io.File

open class BuildKonfigPlugin : Plugin<Project> {


    override fun apply(target: Project) {

        var isMultiplatform = false
        target.plugins.all { p ->
            if (p is KotlinMultiplatformPluginWrapper) {
                isMultiplatform = true
            }
        }

        println("isMultiplatform: $isMultiplatform")

        val objectFactory = target.objects

        val extension = target.extensions.create("buildkonfig", BuildKonfigExtension::class.java)

        val logger = target.logger

        extension.defaultConfigs = objectFactory.newInstance(PlatformConfigDsl::class.java, "defaults", logger)
        extension.targetConfigs = target.container(
            PlatformConfigDsl::class.java,
            PlatformConfigFactory(objectFactory, logger)
        )

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

    fun configure(project: Project, extension: BuildKonfigExtension) {
        val outputDirectory = File(project.buildDir, "buildkonfig")
        val commonOutputDirectory = File(outputDirectory, "commonMain")

        val mppExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val targets = mppExtension.targets
        val sourceSets = mppExtension.sourceSets
        targets.filter { it.name != "metadata" }.forEach { target ->
            val sourceSetMain = sourceSets.getByName("${target.name}Main")
            val outDirMain = File(outputDirectory, "${target.name}Main")

            sourceSetMain.kotlin.srcDirs(outDirMain.toRelativeString(project.projectDir))
        }

        sourceSets.getByName("commonMain").kotlin
            .srcDirs(commonOutputDirectory.toRelativeString(project.projectDir))


        project.afterEvaluate { p ->

            p.extensions.getByType(KotlinMultiplatformExtension::class.java).targets.forEach { target ->
                println("----")
                println("target: $target, ${target.name}, ${target.platformType.name}")
                target.compilations.forEach { compilationUnit ->
                    println("compilation: $compilationUnit, ${compilationUnit::class}")
                    if (compilationUnit is KotlinNativeCompilation) {
                        val outDirName = "${target.name}Main"
                        val compilationType = compilationUnit.name
                        val task = registerGenerateBuildKonfigTask(
                            p,
                            target,
                            compilationType,
                            compilationUnit,
                            extension,
                            commonOutputDirectory,
                            File(outputDirectory, outDirName)
                        )

                        compilationUnit.target.binaries.forEach { binary ->
                            p.tasks.named(binary.linkTaskName).configure { it.dependsOn(task) }
                        }
                    } else if (compilationUnit is KotlinJvmAndroidCompilation) {
                        println("android compilation: ${compilationUnit.name}, ${compilationUnit.compilationName}")
                        val outDirName = "${target.name}Main"
                        val compilationType = if (compilationUnit.name.endsWith("Test")) "test" else "main"

                        val task = registerGenerateBuildKonfigTask(
                            p,
                            target,
                            compilationType,
                            compilationUnit,
                            extension,
                            commonOutputDirectory,
                            File(outputDirectory, outDirName)
                        )
                        p.tasks.named(compilationUnit.compileKotlinTaskName).configure { it.dependsOn(task) }
                    } else {
                        val outDirName = "${target.name}Main"
                        val compilationType = compilationUnit.name

                        val task = registerGenerateBuildKonfigTask(
                            p,
                            target,
                            compilationType,
                            compilationUnit,
                            extension,
                            commonOutputDirectory,
                            File(outputDirectory, outDirName)
                        )
                        p.tasks.named(compilationUnit.compileKotlinTaskName).configure { it.dependsOn(task) }
                    }
                }
            }
        }
    }

    fun registerGenerateBuildKonfigTask(
        project: Project,
        target: KotlinTarget,
        compilationType: String,
        compilation: KotlinCompilation<*>,
        extension: BuildKonfigExtension,
        commonOutputDirectory: File,
        outputDirectory: File
    ): TaskProvider<BuildKonfigTask> {
        val taskName = getTaskName(target, compilation)
        println("BuildKonfig task: $taskName")
        return project.tasks.register(taskName, BuildKonfigTask::class.java) {
            it.setExtension(extension)
            it.targetName = target.targetName
            it.compilationType = compilationType
            it.platformType = target.platformType
            it.commonOutputDirectory = commonOutputDirectory
            it.outputDirectory = outputDirectory
            it.group = "buildkonfig"
            it.description =
                "Generate BuildKonfig for ${target.targetName} - ${target.platformType} - ${compilation.name}"

            println("task output: ${it.outputDirectory}")
        }
    }

    fun getUniqueIdentifier(
        target: KotlinTarget,
        compilation: KotlinCompilation<*>
    ): String {
        return "${compilation.name.decapitalize()}${target.name.capitalize()}${target.platformType.name.capitalize()}"
    }

    fun getTaskName(target: KotlinTarget, compilation: KotlinCompilation<*>): String {
        return "generate${getUniqueIdentifier(target, compilation).capitalize()}BuildKonfig"
    }
}
