package com.codingfeline.buildkonfig.compiler

import java.io.File

data class TargetConfigFile(
    val platformType: String,
    val outputDirectory: File,
    val config: TargetConfig
)
