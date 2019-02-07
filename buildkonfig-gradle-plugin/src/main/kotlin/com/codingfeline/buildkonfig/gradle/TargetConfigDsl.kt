package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.compiler.TargetConfig
import org.gradle.api.logging.Logger
import java.io.Serializable
import javax.inject.Inject

open class TargetConfigDsl @Inject constructor(
    name: String,
    private val logger: Logger
) : TargetConfig(name), Serializable {

    companion object {
        const val serialVersionUID = 1L
    }

    @Suppress("unused")
    fun buildConfigField(
        type: FieldSpec.Type,
        name: String,
        value: String
    ) {

        val alreadyPresent = fieldSpecs[name]

        if (alreadyPresent != null) {
            logger.info("TargetConfig: buildConfigField '$name' is being replaced: ${alreadyPresent.value} -> $value")
        }
        fieldSpecs[name] = FieldSpec(type, name, value)
    }

    fun toTargetConfig(): TargetConfig {
        val config = TargetConfig(name)
            .also {
                it.flavor = this.flavor
                it.fieldSpecs.putAll(this.fieldSpecs)
            }

        return config
    }
}
