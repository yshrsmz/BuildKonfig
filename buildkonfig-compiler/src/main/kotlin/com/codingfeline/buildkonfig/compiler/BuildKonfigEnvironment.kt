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

        try {
            BuildKonfigCompiler.compileCommon(data.packageName, data.commonConfig, writer, logger)
        } catch (e: Throwable) {
            e.message?.let { errors.add(it) }
        }

        data.targetConfigs.forEach { config ->
            try {
                BuildKonfigCompiler.compileTarget(data.packageName, config, writer, logger)
            } catch (e: Throwable) {
                e.message?.let { errors.add(it) }
            }
        }

        return if (errors.isEmpty()) {
            CompilationStatus.Success()
        } else {
            CompilationStatus.Failure(errors)
        }
    }
}
