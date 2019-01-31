package com.codingfeline.buildkonfig.compiler

import com.codingfeline.buildkonfig.compiler.generator.BuildKonfigCompiler
import java.io.File

class BuildKonfigEnvironment(
    val data: BuildKonfigData
) {

    sealed class CompilationStatus {
        class Success : CompilationStatus()
        class Failure(val errors: List<String>) : CompilationStatus()
    }

    fun generateConfigs(logger: Logger): CompilationStatus {
        val errors = ArrayList<String>()

        val writer = writer@{ fileName: String ->
            val file = File(fileName)
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
            return@writer file.writer()
        }

        BuildKonfigCompiler.compileCommon(data.packageName, data.commonConfig, writer, logger)

        BuildKonfigCompiler.compileTarget(data.packageName, data.targetConfig, writer, logger)

        return CompilationStatus.Success()
    }
}
