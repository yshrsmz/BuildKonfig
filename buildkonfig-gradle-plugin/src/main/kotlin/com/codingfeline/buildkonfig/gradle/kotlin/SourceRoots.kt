package com.codingfeline.buildkonfig.gradle.kotlin

import com.codingfeline.buildkonfig.gradle.TargetBuildKonfig
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

internal fun TargetBuildKonfig.sources(): List<Source> {
    return target.sources()
}

internal fun KotlinTarget.sources(): List<Source> {
    return compilations.mapNotNull { compilation ->
        if (compilation.name.endsWith(suffix = "Test", ignoreCase = true)) {
            return@mapNotNull null
        }
        val targetName = if (this is KotlinMetadataTarget) "common" else this.targetName
        Source(
            type = this.platformType,
            nativePresetName = (this as? KotlinNativeTarget)?.preset?.name,
            name = "$targetName${compilation.name.capitalize()}",
            variantName = (compilation as? KotlinJvmAndroidCompilation)?.name,
            sourceDirectorySet = compilation.defaultSourceSet.kotlin,
            sourceSets = compilation.allKotlinSourceSets.map { it.name },
        )
    }
}

internal data class Source(
    val type: KotlinPlatformType,
    val nativePresetName: String? = null,
    val sourceDirectorySet: SourceDirectorySet,
    val name: String,
    val variantName: String? = null,
    val sourceSets: List<String>
)
