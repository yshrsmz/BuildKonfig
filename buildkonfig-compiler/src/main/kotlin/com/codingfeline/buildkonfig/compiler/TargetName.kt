package com.codingfeline.buildkonfig.compiler

import java.io.Serializable


data class TargetName(
    val name: String,
    val platformType: PlatformType
) : Serializable

/**
 * see org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
 */
enum class PlatformType {
    common, jvm, js, androidJvm, native;
}