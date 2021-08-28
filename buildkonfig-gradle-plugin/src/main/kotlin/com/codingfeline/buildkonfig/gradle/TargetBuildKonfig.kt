package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.compiler.TargetName
import com.codingfeline.buildkonfig.gradle.kotlin.sources
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import java.io.File

data class TargetBuildKonfig(
    val project: Project,
    val objectProperties: BuildKonfigObjectPropertiesImpl,
    val target: KotlinTarget,
    // { [flavor: string]: TargetConfigDsl }
    val defaultConfigs: Map<String, TargetConfigDsl>,
    // { [flavor: string]: TargetConfigDsl }
    val targetConfigs: Map<String, TargetConfigDsl>
) {
    private val sources by lazy { sources() }

    private val outputDirectory by lazy {
        listOf("buildkonfig", "${target.targetName}Main")
            .fold(project.buildDir) { acc, path -> File(acc, path) }
    }

    fun registerTask() {
        val task = project.tasks.register(
            "generate${target.targetName}MainBuildKonfig",
            BuildKonfigTask2::class.java
        ) { t ->
            t.outputDirectory = outputDirectory.also { it.mkdirs() }
            t.objectProperties = objectProperties
            t.target = TargetName(target.targetName, target.platformType.toPlatformType())
            t.defaultConfigs = defaultConfigs
            t.targetConfigs = targetConfigs
        }
        sources.forEach { s -> s.sourceDirectorySet.srcDir(task.map { it.outputDirectory!! }) }
        project.tasks.named("generateBuildKonfig").configure { it.dependsOn(task) }
    }
}
