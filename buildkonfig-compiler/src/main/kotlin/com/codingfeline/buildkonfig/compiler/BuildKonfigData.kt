package com.codingfeline.buildkonfig.compiler

data class BuildKonfigData(
    val objectProperties: BuildKonfigObjectProperties,
    // field specs for common source set
    val commonConfig: TargetConfigFile,
    // field specs for target source set
    val targetConfigs: List<TargetConfigFile>
) {
    val hasJsTarget: Boolean = targetConfigs.any { it.isJsTarget }
    val hasTargetSpecificConfigs: Boolean = targetConfigs.any { it.config != null }
}
