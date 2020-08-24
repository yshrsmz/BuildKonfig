package com.codingfeline.buildkonfig.compiler.generator

import com.codingfeline.buildkonfig.compiler.Logger
import com.codingfeline.buildkonfig.compiler.TargetConfigFile
import com.squareup.kotlinpoet.FileSpec
import java.io.Closeable

typealias FileAppender = (fileName: String) -> Appendable

object BuildKonfigCompiler {

    fun compileCommonObject(
        packageName: String,
        objectName: String,
        exposeObject: Boolean,
        configFile: TargetConfigFile,
        hasJsTarget: Boolean,
        output: FileAppender,
        logger: Logger
    ) {
        val outputDirectory = getOutputDirectory(configFile, packageName)

        val konfigType =
            BuildKonfigGenerator.ofCommonObject(configFile, exposeObject, hasJsTarget, logger).generateType(objectName)

        FileSpec.builder(packageName, objectName)
            .apply {
                addType(konfigType)
            }
            .build()
            .writeToAndClose(output("$outputDirectory/$objectName.kt"))
    }

    fun compileCommon(
        packageName: String,
        objectName: String,
        exposeObject: Boolean,
        configFile: TargetConfigFile,
        output: FileAppender,
        logger: Logger
    ) {
        val outputDirectory = getOutputDirectory(configFile, packageName)

        val konfigType = BuildKonfigGenerator.ofCommon(configFile, exposeObject, logger).generateType(objectName)

        FileSpec.builder(packageName, objectName)
            .apply {
                addType(konfigType)
            }
            .build()
            .writeToAndClose(output("$outputDirectory/$objectName.kt"))
    }

    fun compileTarget(
        packageName: String,
        objectName: String,
        exposeObject: Boolean,
        configFile: TargetConfigFile,
        output: FileAppender,
        logger: Logger
    ) {
        val outputDirectory = getOutputDirectory(configFile, packageName)
        val konfigType = BuildKonfigGenerator.ofTarget(configFile, exposeObject, logger).generateType(objectName)

        FileSpec.builder(packageName, objectName)
            .apply {
                addType(konfigType)
            }
            .build()
            .writeToAndClose(output("$outputDirectory/$objectName.kt"))
    }

    private fun FileSpec.writeToAndClose(appendable: Appendable) {
        writeTo(appendable)
        if (appendable is Closeable) appendable.close()
    }

    private fun getPackageDirectory(packageName: String): String {
        return packageName.replace(".", "/")
    }

    private fun getOutputDirectory(configFile: TargetConfigFile, packageName: String): String {
        return "${configFile.outputDirectory.absolutePath}/${getPackageDirectory(packageName)}"
    }
}