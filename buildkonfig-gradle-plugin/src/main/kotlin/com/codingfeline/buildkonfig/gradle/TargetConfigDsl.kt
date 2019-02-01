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

    fun buildConfigField(
        type: FieldSpec.Type,
        name: String,
        value: String
    ) {

        val alreadyPresent = fieldSpecs[name]

        if (alreadyPresent != null) {
            logger.info("TargetConfig: buildConfigField '$name' is being replaced: ${alreadyPresent.value} -> $value")
        }
        fieldSpecs.put(name, FieldSpec(type, name, value))
    }

    fun toPlatformConfig(): TargetConfig {
        val config = TargetConfig(name)
        config.fieldSpecs.putAll(this.fieldSpecs)

        return config
    }
}
