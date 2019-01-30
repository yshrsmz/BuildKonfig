package com.codingfeline.buildkonfig.gradle


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBinary
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
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

        project.afterEvaluate { p ->
            //            val task = p.tasks.register("generateBuildKonfig", BuildKonfigTask::class.java) {
//                it.setExtension(extension)
//                it.outputDirectory = outputDirectory
//                it.group = "buildKonfig"
//                it.description = "Generate BuildKonfig"
//            }

            p.extensions.getByType(KotlinMultiplatformExtension::class.java).targets.forEach { target ->
                println("----")
                println("target: $target")
                target.compilations.forEach { compilationUnit ->
                    println("compilation: $compilationUnit, ${compilationUnit::class}")
                    if (compilationUnit is KotlinNativeCompilation) {
                        compilationUnit.target.binaries.forEach { binary ->
                            val taskName = getNativeTaskName(target, compilationUnit, binary)
                            println("binary: ${binary.buildType}")
                            println("buildKonfig task: $taskName")

                            val task = p.tasks.register(taskName, BuildKonfigTask::class.java) {
                                it.setExtension(extension)
                                it.target = target.targetName
                                it.isDebug = binary.buildType == NativeBuildType.DEBUG
                                it.outputDirectory =
                                    File(outputDirectory, getNativeUniqueIdentifier(target, compilationUnit, binary))
                                it.group = "buildKonfig"
                                it.description =
                                    "Generate BuildKonfig for ${target.name} - ${compilationUnit.name} - ${binary.buildType.name}"

                                println("task output: ${it.outputDirectory}")
                            }
                            p.tasks.named(binary.linkTaskName).configure { it.dependsOn(task) }
                        }
                    } else if (compilationUnit is KotlinJvmAndroidCompilation) {
                        println("android compilation: ${compilationUnit.name}, ${compilationUnit.compilationName}")
                        val taskName = getTaskName(target, compilationUnit)
                        val task = p.tasks.register(taskName, BuildKonfigTask::class.java) {
                            it.setExtension(extension)
                            it.target = target.name
                            it.isDebug = compilationUnit.name.contains("debug", true)
                            it.outputDirectory = File(outputDirectory, getUniqueIdentifier(target, compilationUnit))
                            it.group = "buildKonfig"
                            it.description = "Generate BuildKonfig for ${target.name} - ${compilationUnit.name}"

                            println("task output: ${it.outputDirectory}")
                        }
                        p.tasks.named(compilationUnit.compileKotlinTaskName).configure { it.dependsOn(task) }
                    } else {
                        val taskName = getTaskName(target, compilationUnit)
                        val task = p.tasks.register(taskName, BuildKonfigTask::class.java) {
                            it.setExtension(extension)
                            it.target = target.targetName
                            it.isDebug = false
                            it.outputDirectory = File(outputDirectory, getUniqueIdentifier(target, compilationUnit))
                            it.group = "buildKonfig"
                            it.description = "Generate BuildKonfig for ${target.name} - ${compilationUnit.name}"

                            println("task output: ${it.outputDirectory}")
                        }
                        p.tasks.named(compilationUnit.compileKotlinTaskName).configure { it.dependsOn(task) }
                    }
                }
            }
        }
    }

    fun getNativeUniqueIdentifier(
        target: KotlinTarget,
        compilation: KotlinNativeCompilation,
        binary: NativeBinary
    ): String {
        return "${compilation.name.decapitalize()}${binary.buildType.name.toLowerCase().capitalize()}${target.name.capitalize()}${binary.outputKind.name.toLowerCase().capitalize()}"
    }

    fun getNativeTaskName(target: KotlinTarget, compilation: KotlinNativeCompilation, binary: NativeBinary): String {
        return "generate${getNativeUniqueIdentifier(target, compilation, binary).capitalize()}BuildKonfig"
    }

    fun getUniqueIdentifier(target: KotlinTarget, compilation: KotlinCompilation<*>): String {
        return "${compilation.name.decapitalize()}${target.name.capitalize()}"
    }

    fun getTaskName(target: KotlinTarget, compilation: KotlinCompilation<*>): String {
        return "generate${getUniqueIdentifier(target, compilation)}BuildKonfig"
    }
}
