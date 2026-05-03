package com.codingfeline.buildkonfig.compiler

import com.codingfeline.buildkonfig.compiler.generator.BuildKonfigCompiler
import com.codingfeline.buildkonfig.compiler.generator.FileAppender
import java.io.File

class BuildKonfigEnvironment(
    private val data: BuildKonfigData
) {

    sealed class CompilationStatus {
        object Success : CompilationStatus()
        class Failure(val errors: List<String>) : CompilationStatus()
    }

    fun generateConfigs(logger: BuildKonfigLogger): CompilationStatus {
        val errors = ArrayList<String>()

        val writer = writer@{ fileName: String ->
            val file = File(fileName)
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
            return@writer file.writer()
        }

        if (data.hasTargetSpecificConfigs) {
            compileExpectActual(data, writer, logger)
        } else {
            compileCommonObject(data, writer, logger)
        }

        return if (errors.isEmpty()) {
            CompilationStatus.Success
        } else {
            CompilationStatus.Failure(errors)
        }
    }

    private fun compileCommonObject(data: BuildKonfigData, writer: FileAppender, logger: BuildKonfigLogger): List<String> {
        val errors = mutableListOf<String>()
        try {
            BuildKonfigCompiler.compileCommonObject(
                data.packageName,
                data.objectName,
                data.exposeObject,
                data.commonConfig,
                data.hasJsTarget,
                writer,
                logger
            )
        } catch (e: Throwable) {
            e.message?.let { errors.add(it) }
        }
        return errors
    }

    private fun compileExpectActual(data: BuildKonfigData, writer: FileAppender, logger: BuildKonfigLogger): List<String> {
        val errors = mutableListOf<String>()

        warnConstFieldsInExpectActual(data, logger)

        try {
            BuildKonfigCompiler.compileCommon(
                data.packageName,
                data.objectName,
                data.exposeObject,
                data.commonConfig,
                writer,
                logger
            )
        } catch (e: Throwable) {
            e.message?.let { errors.add(it) }
        }

        data.targetConfigs.filter { it.config != null }
            .forEach { config ->
                try {
                    BuildKonfigCompiler.compileTarget(
                        data.packageName,
                        data.objectName,
                        data.exposeObject,
                        config,
                        writer,
                        logger
                    )
                } catch (e: Throwable) {
                    e.message?.let { errors.add(it) }
                }
            }
        return errors
    }

    private fun warnConstFieldsInExpectActual(data: BuildKonfigData, logger: BuildKonfigLogger) {
        val commonConfig = data.commonConfig.config
        if (commonConfig == null) {
            return
        }

        val constFieldNames = commonConfig.fieldSpecs.values
            .filter { it.const }
            .map { it.name }

        if (constFieldNames.isEmpty()) {
            return
        }

        logger.warn(
            "BuildKonfig: const = true is not honored on the common (expect) side when target-specific " +
                "configs are present (K2 compiler restricts `expect const val`). The expect declaration " +
                "is emitted as `val`, while each target keeps `actual const val`. Affected field(s): " +
                "${constFieldNames.joinToString(", ")}. Common code cannot reference these as compile-time " +
                "constants; use them as constants only from target-specific source sets."
        )
    }
}
