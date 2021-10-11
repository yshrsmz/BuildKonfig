package com.codingfeline.buildkonfig.compiler

import java.io.Serializable

open class TargetConfig(val name: String) : Serializable {
    var flavor: String = ""
    val fieldSpecs = mutableMapOf<String, FieldSpec>()

    fun copyFieldSpecs(): Map<String, FieldSpec> = fieldSpecs.mapValues { it.value.copy() }

    fun copy(): TargetConfig = TargetConfig(name).also { it.fieldSpecs.putAll(copyFieldSpecs()) }
}
