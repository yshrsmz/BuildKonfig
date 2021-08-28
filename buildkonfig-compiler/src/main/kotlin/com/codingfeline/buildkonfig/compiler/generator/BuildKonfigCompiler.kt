package com.codingfeline.buildkonfig.compiler.generator

import com.codingfeline.buildkonfig.compiler.BuildKonfigObjectProperties
import com.codingfeline.buildkonfig.compiler.Logger
import com.codingfeline.buildkonfig.compiler.TargetConfigFile
import com.squareup.kotlinpoet.FileSpec
import java.io.Closeable

typealias FileAppender = (fileName: String) -> Appendable

object BuildKonfigCompiler {

    fun compileCommonObject(
        objectProps: BuildKonfigObjectProperties,
        configFile: TargetConfigFile,
        hasJsTarget: Boolean,
        output: FileAppender,
        logger: Logger
    ) {
        val outputDirectory = getOutputDirectory(configFile, objectProps.packageName)

        val konfigType = BuildKonfigGenerator.ofCommonObject(configFile, objectProps.exposeObject, hasJsTarget, logger)
            .generateType(objectProps.objectName)

        FileSpec.builder(objectProps.packageName, objectProps.objectName)
            .apply {
                addType(konfigType)
            }
            .build()
            .writeToAndClose(output("$outputDirectory/${objectProps.objectName}.kt"))
    }

    fun compileCommon(
        objectProps: BuildKonfigObjectProperties,
        configFile: TargetConfigFile,
        output: FileAppender,
        logger: Logger
    ) {
        val outputDirectory = getOutputDirectory(configFile, objectProps.packageName)

        val konfigType = BuildKonfigGenerator.ofCommon(configFile, objectProps.exposeObject, logger)
            .generateType(objectProps.objectName)

        FileSpec.builder(objectProps.packageName, objectProps.objectName)
            .apply {
                addType(konfigType)
            }
            .build()
            .writeToAndClose(output("$outputDirectory/${objectProps.objectName}.kt"))
    }

    fun compileTarget(
        objectProps: BuildKonfigObjectProperties,
        configFile: TargetConfigFile,
        output: FileAppender,
        logger: Logger
    ) {
        val outputDirectory = getOutputDirectory(configFile, objectProps.packageName)
        val konfigType = BuildKonfigGenerator.ofTarget(configFile, objectProps.exposeObject, logger)
            .generateType(objectProps.objectName)

        FileSpec.builder(objectProps.packageName, objectProps.objectName)
            .apply {
                addType(konfigType)
            }
            .build()
            .writeToAndClose(output("$outputDirectory/${objectProps.objectName}.kt"))
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
