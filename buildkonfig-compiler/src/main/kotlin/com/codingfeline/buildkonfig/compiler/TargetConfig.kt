package com.codingfeline.buildkonfig.compiler

import java.io.Serializable

open class TargetConfig(val name: String) : Serializable {

    var target: String? = null

    val fieldSpecs = mutableMapOf<String, FieldSpec>()
}
