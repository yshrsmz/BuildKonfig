package com.codingfeline.buildkonfig

import java.io.Serializable

open class PlatformConfig(val name: String) : Serializable {

    val fieldSpecs = mutableMapOf<String, FieldSpec>()
}
