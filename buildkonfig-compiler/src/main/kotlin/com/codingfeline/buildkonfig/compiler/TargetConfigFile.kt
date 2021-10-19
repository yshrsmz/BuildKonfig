package com.codingfeline.buildkonfig.compiler

import java.io.File
import java.io.Serializable

interface TargetConfigFile : Serializable {
    val targetName: TargetName
    val outputDirectory: File
    val config: TargetConfig?
}
