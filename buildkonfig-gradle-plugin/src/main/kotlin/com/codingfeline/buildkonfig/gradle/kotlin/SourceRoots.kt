package com.codingfeline.buildkonfig.gradle.kotlin

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
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

internal fun KotlinProjectExtension.sources(): List<Source> {
    return (when (this) {
        is KotlinSingleTargetExtension<*> -> sourcesForTarget(target)
            .toMutableSet() // Same as `.distinct()` but without converting into a `List`

        is KotlinMultiplatformExtension -> targets
            .flatMapTo(LinkedHashSet()) { sourcesForTarget(it) } // Same as `flatMap { … }.distinct().toMutableSet()` without creating intermediate collections

        else -> error("Unexpected 'kotlin' extension $this") /** @see org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.targets */
    }).sortedBy { it.sourceSets.size }
}

private fun sourcesForTarget(target: KotlinTarget) = target.compilations
    .filter { !it.name.endsWith(suffix = "Test", ignoreCase = true) }
    .map { compilation ->
        val (defaultSourceSet, sourceSets) = when (target.platformType) {
            KotlinPlatformType.androidJvm -> {
                val mppMainSourceSetName = "${target.name}Main"
                val default = compilation.allKotlinSourceSets
                    .filter { val name = it.name; name == mppMainSourceSetName || name == "main" }
                    // TODO Consider logging an error instead of throwing here?
                    .run { firstOrNull { it.name == mppMainSourceSetName } ?: first() }

                val defaultSourceSetName = default.name
                val compilationDefaultSourceSetName = compilation.defaultSourceSet.name
                val all = compilation.allKotlinSourceSets
                    .filter {
                        val name = it.name
                        name != defaultSourceSetName &&
                                name != compilationDefaultSourceSetName
                    }

                default to all
            }
            else -> {
                val defaultSourceSet = compilation.defaultSourceSet
                defaultSourceSet to compilation.allKotlinSourceSets
                    .filter { it != defaultSourceSet }
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
