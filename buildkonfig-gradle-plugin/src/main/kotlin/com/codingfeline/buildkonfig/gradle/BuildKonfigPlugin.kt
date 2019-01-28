package com.codingfeline.buildkonfig.gradle


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBinary
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

        if (!isMultiplatform) {
            throw IllegalStateException(
                "BuildKonfig Gradle plugin applied in project '${target.path}' " +
                        "but no supported Kotlin plugin was found"
            )
        }

        val objectFactory = target.objects

        val extension = target.extensions.create("buildKonfig", BuildKonfigExtension::class.java)

        val logger = Logging.getLogger(BuildKonfigPlugin::class.java)

        extension.defaultConfigs = objectFactory.newInstance(PlatformConfigDsl::class.java, "defaults", logger)
        extension.targetConfigs = target.container(
            PlatformConfigDsl::class.java,
            PlatformConfigFactory(objectFactory, logger)
        )

        configure(target, extension)
    }

    fun configure(project: Project, extension: BuildKonfigExtension) {
        val outputDirectory = File(project.buildDir, "buildKonfig")

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
                                it.group = "buildKonfig"
                                it.description =
                                    "Generate BuildKonfig for ${target.name} - ${compilationUnit.name} - ${binary.buildType.name}"
                            }
                            p.tasks.named(binary.linkTaskName).configure { it.dependsOn(task) }
                        }
                    } else {
                        val taskName = getTaskName(target, compilationUnit)
                        val task = p.tasks.register(taskName, BuildKonfigTask::class.java) {
                            it.setExtension(extension)
                            it.group = "buildKonfig"
                            it.description = "Generate BuildKonfig for ${target.name} - ${compilationUnit.name}"
                        }
                        p.tasks.named(compilationUnit.compileKotlinTaskName).configure { it.dependsOn(task) }
                    }
                }
            }
        }
    }

    fun getNativeTaskName(target: KotlinTarget, compilation: KotlinNativeCompilation, binary: NativeBinary): String {
        return "generate${compilation.name.capitalize()}${binary.buildType.name.toLowerCase().capitalize()}${target.name.capitalize()}${binary.outputKind.name.toLowerCase().capitalize()}BuildKonfig"
    }

    fun getTaskName(target: KotlinTarget, compilation: KotlinCompilation<*>): String {
        return "generate${compilation.name.capitalize()}${target.name.capitalize()}BuildKonfig"
    }
}
