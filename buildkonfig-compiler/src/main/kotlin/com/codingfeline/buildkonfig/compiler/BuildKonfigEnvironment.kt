package com.codingfeline.buildkonfig.compiler

import java.io.File

class BuildKonfigEnvironment(
    val platformConfig: PlatformConfig,
    val outputDirectory: File? = null
) {

    sealed class CompilationStatus {
        class Success : CompilationStatus()
        class Failure(val errors: List<String>) : CompilationStatus()
    }

    fun generate(): CompilationStatus {
        val errors = ArrayList<String>()

        val writer = writer@{ fileName: String ->
            val file = File(fileName)
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
            return@writer file.writer()
        }

        return CompilationStatus.Success()
    }
}
