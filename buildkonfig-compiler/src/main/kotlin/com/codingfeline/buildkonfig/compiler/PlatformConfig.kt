package com.codingfeline.buildkonfig.compiler

import java.io.Serializable

open class PlatformConfig(val name: String) : Serializable {

    val fieldSpecs = mutableMapOf<String, FieldSpec>()
}
