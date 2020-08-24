package com.codingfeline.buildkonfig.compiler

import java.io.File

data class TargetConfigFile(
    val targetName: TargetName,
    val outputDirectory: File,
    val config: TargetConfig?
) {
    val isJsTarget: Boolean = targetName.platformType == PlatformType.js
}
