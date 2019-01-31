package com.codingfeline.buildkonfig.compiler.generator

import com.codingfeline.buildkonfig.compiler.KONFIG_OBJECT_NAME
import com.codingfeline.buildkonfig.compiler.Logger
import com.codingfeline.buildkonfig.compiler.TargetConfigFile
import com.squareup.kotlinpoet.FileSpec
import java.io.Closeable

private typealias FileAppender = (fileName: String) -> Appendable

object BuildKonfigCompiler {

    fun compileCommon(
        packageName: String,
        configFile: TargetConfigFile,
        output: FileAppender,
        logger: Logger
    ) {
        val outputDirectory = "${configFile.outputDirectory.absolutePath}/${packageName.replace(".", "/")}"

        val konfigType = BuildKonfigGenerator.ofCommon(configFile, logger).generateType()

        FileSpec.builder(packageName, KONFIG_OBJECT_NAME)
            .apply {
                addType(konfigType)
            }
            .build()
            .writeToAndClose(output("$outputDirectory/$KONFIG_OBJECT_NAME.kt"))
    }

    fun compileTarget(
        packageName: String,
        configFile: TargetConfigFile,
        output: FileAppender,
        logger: Logger
    ) {
        val outputDirectory = "${configFile.outputDirectory.absolutePath}/${packageName.replace(".", "/")}"
        val konfigType = BuildKonfigGenerator.ofTarget(configFile, logger).generateType()

        FileSpec.builder(packageName, KONFIG_OBJECT_NAME)
            .apply {
                addType(konfigType)
            }
            .build()
            .writeToAndClose(output("$outputDirectory/$KONFIG_OBJECT_NAME.kt"))
    }

    private fun FileSpec.writeToAndClose(appendable: Appendable) {
        writeTo(appendable)
        if (appendable is Closeable) appendable.close()
    }
}