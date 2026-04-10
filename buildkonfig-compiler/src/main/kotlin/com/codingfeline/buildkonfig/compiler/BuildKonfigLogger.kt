package com.codingfeline.buildkonfig.compiler

fun interface BuildKonfigLogger {
    fun log(level: LogLevel, message: String)

    fun info(message: String) = log(LogLevel.INFO, message)
    fun warn(message: String) = log(LogLevel.WARN, message)
}

enum class LogLevel { INFO, WARN }
