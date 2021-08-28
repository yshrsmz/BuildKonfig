package com.codingfeline.buildkonfig.gradle


import com.codingfeline.buildkonfig.compiler.PlatformType
import com.codingfeline.buildkonfig.compiler.TargetName
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

abstract class BuildKonfigPlugin : Plugin<Project> {
    private val android = AtomicBoolean(false)
    private val kotlin = AtomicBoolean(false)

    private lateinit var extension: BuildKonfigExtension

    override fun apply(target: Project) {
        extension = target.extensions.create("buildkonfig", BuildKonfigExtension::class.java, target)

        val androidPluginHandler = { _: Plugin<*> ->
            android.set(true)
            target.afterEvaluate {
                target.setupBuildKonfigTasks(afterAndroid = true)
            }
        }
        target.plugins.withId("com.android.application", androidPluginHandler)
        target.plugins.withId("com.android.library", androidPluginHandler)
        target.plugins.withId("com.android.instantapp", androidPluginHandler)
        target.plugins.withId("com.android.feature", androidPluginHandler)
        target.plugins.withId("com.android.dynamic-feature", androidPluginHandler)

        val kotlinPluginHandler = { _: Plugin<*> -> kotlin.set(true) }
        target.plugins.withId("org.jetbrains.kotlin.multiplatform", kotlinPluginHandler)
//        target.plugins.withId("org.jetbrains.kotlin.android", kotlinPluginHandler)
//        target.plugins.withId("org.jetbrains.kotlin.jvm", kotlinPluginHandler)
//        target.plugins.withId("kotlin2js", kotlinPluginHandler)

        target.afterEvaluate {
            target.setupBuildKonfigTasks(afterAndroid = false)
        }
    }

    private fun Project.setupBuildKonfigTasks(afterAndroid: Boolean) {
        if (android.get() && !afterAndroid) return

        check(kotlin.get()) {
            "BuildKonfig Gradle plugin applied in project '${project.path}', but no supported Kotlin plugin was found"
        }

        val isMultiplatform = project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
        check(isMultiplatform) {
            "BuildKonfig Gradle plugin applied in project '${project.path}', but no supported Kotlin multiplatform plugin was found"
        }

        val kmpExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val sourceSets = kmpExtension.sourceSets
        val targets = kmpExtension.targets
        val hasJsTarget = targets.any { it.platformType === KotlinPlatformType.js }

        extension.run {

            project.tasks.register("generateBuildKonfig") {
                it.group = GROUP
                it.description = "Aggregation task which runs every BuildKonfig generation tasks"
            }

            val outputRoot = File(project.buildDir, "buildkonfig")
            if (targetConfigs.isEmpty()) {
                // register common-object task
                val common = targets.single { it.platformType === KotlinPlatformType.common }
                registerCommonObjectTask(outputRoot, common, sourceSets)
            } else {
                // register common-expect task and target-actual tasks
                targets.forEach { target ->
                    println("target: ${target.targetName}, ${target.platformType}, ${target.compilations.map { it.name }}")
                    val config = extension.getTargetBuildKonfig(target)
                    config.registerTask()
                }
            }

        }
    }

    internal fun BuildKonfigExtension.registerCommonObjectTask(
        outputRoot: File,
        commonTarget: KotlinTarget,
        sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
    ) {
        val sourceName = "commonMain"
        val outputDir = File(outputRoot, sourceName)
        outputDir.mkdirs()

        val source = sourceSets.getByName(sourceName)
    }

    internal companion object {
        const val GROUP = "buildkonfig"
    }
}


@Suppress("unused")
open class BuildKonfigPluginBk : Plugin<Project> {

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
        val sourceSets = mppExtension.sourceSets

        val outputDirectoryMap = mutableMapOf<TargetName, File>()

        sourceSets.getByName("commonMain").kotlin
            .srcDirs(commonOutputDirectory.toRelativeString(project.projectDir))

        targets.filter { it.name != "metadata" }.forEach { target ->
            val name = "${target.name}Main"
            val sourceSetMain = sourceSets.getByName(name)

            val outDirMain = File(outputDirectory, name).also { it.mkdirs() }

            sourceSetMain.kotlin.srcDirs(outDirMain.toRelativeString(project.projectDir))

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

            p.extensions.getByType(KotlinMultiplatformExtension::class.java).targets.forEach { target ->
                target.compilations.forEach { compilationUnit ->
                    when (compilationUnit) {
                        is KotlinNativeCompilation -> {

                            p.tasks.named(compilationUnit.compileAllTaskName).configure { it.dependsOn(task) }
                            p.tasks.named(compilationUnit.compileKotlinTaskName).configure { it.dependsOn(task) }

                            compilationUnit.target.binaries.forEach { binary ->
                                p.tasks.named(binary.linkTaskName).configure { it.dependsOn(task) }
                            }
                        }
                        is KotlinJvmAndroidCompilation -> {
                            p.tasks.named(compilationUnit.compileKotlinTaskName)
                                .configure { it.dependsOn(task) }
                        }
                        else -> p.tasks.named(compilationUnit.compileKotlinTaskName).configure { it.dependsOn(task) }
                    }
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
