package com.codingfeline.buildkonfig.compiler

data class BuildKonfigData(
    val packageName: String,
    val objectName: String,
    val exposeObject: Boolean,
    // filed specs for common source set
    val commonConfig: TargetConfigFile,
    // field specs for target source set
    val targetConfigs: List<TargetConfigFile>
)
