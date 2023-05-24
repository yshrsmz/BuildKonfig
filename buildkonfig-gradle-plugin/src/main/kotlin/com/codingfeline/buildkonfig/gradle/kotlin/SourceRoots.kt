package com.codingfeline.buildkonfig.gradle.kotlin

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

data class Source(
    val type: KotlinPlatformType,
    val nativePresetName: String? = null,
    val defaultSourceSet: KotlinSourceSet,
    val name: String,
    val sourceSets: List<KotlinSourceSet>
)

internal fun KotlinSingleTargetExtension<out KotlinTarget>.sources(): List<Source> {
    return sourcesForTarget(target)
        .distinct()
        .sortedBy { it.sourceSets.size }
}

internal fun KotlinMultiplatformExtension.sources(): List<Source> {
    return targets
        .flatMap { sourcesForTarget(it) }
        .distinct()
        .sortedBy { it.sourceSets.size }
}

private fun sourcesForTarget(target: KotlinTarget) = target.compilations
    .filter { !it.name.endsWith(suffix = "Test", ignoreCase = true) }
    .map { compilation ->
        val (defaultSourceSet, sourceSets) = when (target.platformType) {
            KotlinPlatformType.androidJvm -> {
                val default = compilation.allKotlinSourceSets
                    .first { it.name == "${target.name}Main" }

                val all = compilation.allKotlinSourceSets
                    .filter {
                        it.name != "${target.name}Main" &&
                                it.name != compilation.defaultSourceSet.name
                    }

                default to all
            }
            else -> {
                compilation.defaultSourceSet to compilation.allKotlinSourceSets
                    .filter { it.name != compilation.defaultSourceSet.name }
            }
        }
        Source(
            type = compilation.platformType,
            nativePresetName = ((target as? KotlinNativeTarget)?.preset?.name),
            name = defaultSourceSet.name,
            defaultSourceSet = defaultSourceSet,
            sourceSets = sourceSets
        )
    }
