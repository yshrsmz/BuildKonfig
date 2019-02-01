package com.codingfeline.buildkonfig.compiler

enum class CompilationType {
    MAIN, TEST;

    companion object {
        fun from(name: String): CompilationType {
            return values().first { name.toUpperCase() == it.name }
        }
    }
}
