package com.codingfeline.buildkonfig.compiler.generator

import com.codingfeline.buildkonfig.compiler.Logger
import com.codingfeline.buildkonfig.compiler.TargetConfigFile
import java.io.OutputStreamWriter

object BuildKonfigCompiler {

    fun compileCommon(
        packageName: String,
        configFile: TargetConfigFile,
        writer: (String) -> OutputStreamWriter,
        logger: Logger
    ) {

    }

    fun compileTarget(
        packageName: String,
        configFile: TargetConfigFile,
        writer: (String) -> OutputStreamWriter,
        logger: Logger
    ) {

    }
}