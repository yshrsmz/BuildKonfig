package com.codingfeline.buildkonfig.compiler

import java.io.Serializable


data class TargetName(
    val name: String,
    val platformType: String
) : Serializable
