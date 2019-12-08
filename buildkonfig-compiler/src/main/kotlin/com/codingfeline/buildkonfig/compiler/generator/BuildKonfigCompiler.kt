package com.codingfeline.buildkonfig.compiler.generator

import com.codingfeline.buildkonfig.compiler.KONFIG_OBJECT_NAME
import com.codingfeline.buildkonfig.compiler.Logger
import com.codingfeline.buildkonfig.compiler.TargetConfigFile
import com.squareup.kotlinpoet.FileSpec
import java.io.Closeable

typealias FileAppender = (fileName: String) -> Appendable

object BuildKonfigCompiler {

    fun compileCommonObject(
        packageName: String,
        configFile: TargetConfigFile,
        output: FileAppender,
        logger: Logger
    ) {
        val outputDirectory = getOutputDirectory(configFile, packageName)

        val konfigType = BuildKonfigGenerator.ofCommonObject(configFile, logger).generateType()

        FileSpec.builder(packageName, KONFIG_OBJECT_NAME)
            .apply {
                addType(konfigType)
            }
            .build()
            .writeToAndClose(output("$outputDirectory/$KONFIG_OBJECT_NAME.kt"))
    }

    fun compileCommon(
        packageName: String,
        configFile: TargetConfigFile,
        output: FileAppender,
        logger: Logger
    ) {
        val outputDirectory = getOutputDirectory(configFile, packageName)

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
        val outputDirectory = getOutputDirectory(configFile, packageName)
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

    private fun getPackageDirectory(packageName: String): String {
        return packageName.replace(".", "/")
    }

    private fun getOutputDirectory(configFile: TargetConfigFile, packageName: String): String {
        return "${configFile.outputDirectory.absolutePath}/${packageName.replace(".", "/")}"
    }
}