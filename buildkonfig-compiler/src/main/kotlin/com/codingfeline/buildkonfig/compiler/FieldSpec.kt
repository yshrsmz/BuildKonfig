package com.codingfeline.buildkonfig.compiler

import java.io.Serializable

data class FieldSpec(
    val type: String,
    val name: String,
    val value: String
) : Serializable
