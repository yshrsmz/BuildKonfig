package com.codingfeline.buildkonfig.gradle


import com.codingfeline.buildkonfig.compiler.CompilationType
import com.codingfeline.buildkonfig.compiler.TargetName
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
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

        extension.defaultConfigs = objectFactory.newInstance(TargetConfigDsl::class.java, "defaults", logger)
        extension.targetConfigs = target.container(
            TargetConfigDsl::class.java,
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

        targets.first().platformType
        val targetNames = targets.map { TargetName(name = it.name, platformType = it.platformType.name) }
            .filter { it.name != "metadata" }

        println(targetNames)
        val outputDirectoryMap = mutableMapOf<TargetName, File>()

        sourceSets.getByName("commonMain").kotlin
            .srcDirs(commonOutputDirectory.toRelativeString(project.projectDir))

        targets.filter { it.name != "metadata" }.forEach { target ->
            val name = "${target.name}Main"
            val sourceSetMain = sourceSets.getByName(name)
            val outDirMain = File(outputDirectory, name)

            sourceSetMain.kotlin.srcDirs(outDirMain.toRelativeString(project.projectDir))

            outputDirectoryMap[TargetName(target.name, target.platformType.name)] = outDirMain
        }

        project.afterEvaluate { p ->

            val mainTask = p.tasks.register("generateMainBuildKonfig", BuildKonfigTask::class.java) {
                it.packageName = requireNotNull(extension.packageName) { "packageName must be provided" }
                it.commonOutputDirectory = commonOutputDirectory
                it.outputDirectories = outputDirectoryMap
                it.compilationType = CompilationType.MAIN
                it.extension = extension

                it.group = "buildkonfig"
                it.description = "generate BuildKonfig for main"
            }

            val testTask = p.tasks.register("generateTestBuildKonfig", BuildKonfigTask::class.java) {
                it.packageName = requireNotNull(extension.packageName) { "packageName must be provided" }
                it.commonOutputDirectory = commonOutputDirectory
                it.outputDirectories = outputDirectoryMap
                it.compilationType = CompilationType.TEST
                it.extension = extension

                it.group = "buildkonfig"
                it.description = "generate BuildKonfig for test"
            }

            p.extensions.getByType(KotlinMultiplatformExtension::class.java).targets.forEach { target ->
                println("----")
                println("target: $target, ${target.name}, ${target.platformType.name}")
                target.compilations.forEach { compilationUnit ->
                    println("compilation: $compilationUnit, ${compilationUnit::class}")
                    if (compilationUnit is KotlinNativeCompilation) {
                        val generateTask = if (compilationUnit.name == "main") mainTask else testTask

                        compilationUnit.target.binaries.forEach { binary ->
                            p.tasks.named(binary.linkTaskName).configure { it.dependsOn(generateTask) }
                        }
                    } else if (compilationUnit is KotlinJvmAndroidCompilation) {
                        println("android compilation: ${compilationUnit.name}, ${compilationUnit.compilationName}")
                        val generateTask = if (compilationUnit.name.endsWith("Test")) testTask else mainTask

                        p.tasks.named(compilationUnit.compileKotlinTaskName).configure { it.dependsOn(generateTask) }
                    } else {
                        val generateTask = if (compilationUnit.name == "main") mainTask else testTask

                        p.tasks.named(compilationUnit.compileKotlinTaskName).configure { it.dependsOn(generateTask) }
                    }
                }
            }
        }
    }
}
