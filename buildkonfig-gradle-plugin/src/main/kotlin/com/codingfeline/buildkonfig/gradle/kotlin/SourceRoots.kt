package com.codingfeline.buildkonfig.gradle.kotlin

import com.codingfeline.buildkonfig.compiler.TargetConfig
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

data class Source(
    val type: KotlinPlatformType,
    val nativePresetName: String? = null,
    val defaultSourceSet: KotlinSourceSet,
    val name: String,
    val sourceSets: List<KotlinSourceSet>
)

internal fun KotlinMultiplatformExtension.sources(): List<Source> {
    return targets
        .flatMap { target ->
            target.compilations.filter { !it.name.endsWith(suffix = "Test", ignoreCase = true) }
                .map { compilation ->
                    val defaultSourceSet = when (target.platformType) {
                        KotlinPlatformType.androidJvm -> {
                            compilation.allKotlinSourceSets
                                .first { it.name == "${target.name}Main" }
                        }
                        else -> compilation.defaultSourceSet
                    }
                    Source(
                        type = compilation.platformType,
                        nativePresetName = ((target as? KotlinNativeTarget)?.preset?.name),
                        name = defaultSourceSet.name,
                        defaultSourceSet = defaultSourceSet,
                        sourceSets = flattenSourceSetDependencies(compilation.defaultSourceSet)
                    )
                }
        }
        .distinct()
        .sortedBy { it.sourceSets.size }
}

internal fun decideTargetSourceSets(
    project: Project,
    mainSourceSets: List<KotlinSourceSet>,
    sources: List<Source>,
    targetConfigs: Map<String, TargetConfig>
) {

    val result = mutableMapOf<String, TargetConfig>()

    mainSourceSets.forEach { ss ->
        if (result.containsKey(ss.name)) return@forEach

        val hasTargetConfig = targetConfigs.containsKey(ss.name)
        val dependents = ss.dependsOn.filter { it.name != "commonMain" }
        if (hasTargetConfig) {
            val dependentsWithTargetConfig = dependents.filter { targetConfigs.containsKey(it.name) }
            if (dependentsWithTargetConfig.isNotEmpty()) {
                project.logger.warn(
                    "BuildKonfig configuration for SourceSet(${ss.name}) is ignored, " +
                            "as its dependent SourceSets(${dependentsWithTargetConfig.map { it.name }}) also have configurations"
                )
            } else {

            }
        }

    }
    sources.sortedBy { it.sourceSets.size }
        .forEach { source ->
            val hasTargetConfig = targetConfigs.containsKey(source.name)
            val other = source.sourceSets.filter { it.name == "commonMain" || it.name == source.name }
            if (other.isNotEmpty()) {
                // has dependent SourceSets

            }
        }
}

internal fun findSiblingSourceSets(
    name: String,
    sourceSets: List<KotlinSourceSet>
): List<KotlinSourceSet> {
    if (name == "commonMain") return emptyList()

    val target = sourceSets.first { it.name == name }

    val nonCommonAncestor = target.dependsOn.firstOrNull { it.name != "commonMain" }
    return if (nonCommonAncestor != null) {
        sourceSets.filter { it.dependsOn.contains(nonCommonAncestor) }
    } else {
        sourceSets.filter { it.dependsOn.all { ss -> ss.name == "commonMain" } }
    }
}

internal fun flattenSourceSetDependencies(sourceSet: KotlinSourceSet): List<KotlinSourceSet> {
    if (sourceSet.dependsOn.isEmpty()) return emptyList()
    return sourceSet.dependsOn
        .flatMap { flattenSourceSetDependencies(it) + it }
        .distinct()
}

fun findAncestorWithTargetConfig(
    sourceSet: KotlinSourceSet,
    targetConfigs: Map<String, TargetConfig>
) {


}

fun getMinimumTargets(sourceSets: List<KotlinSourceSet>) {
    sourceSets.sortedBy { it.dependsOn.size }
}

val KotlinSourceSet.isCommonRoot: Boolean
    get() = this.dependsOn.isEmpty()