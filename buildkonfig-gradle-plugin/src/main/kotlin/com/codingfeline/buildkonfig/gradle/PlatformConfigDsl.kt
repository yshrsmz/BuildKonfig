package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.compiler.PlatformConfig
import org.gradle.api.logging.Logger
import java.io.Serializable
import javax.inject.Inject

open class PlatformConfigDsl @Inject constructor(
    name: String,
    private val logger: Logger
) : PlatformConfig(name), Serializable {
    companion object {
        const val serialVersionUID = 1L
    }

    fun buildConfigField(
        type: String,
        name: String,
        value: String
    ) {

        val alreadyPresent = fieldSpecs[name]

        if (alreadyPresent != null) {
            logger.info("PlatformConfig: buildConfigField '$name' is being replaced: ${alreadyPresent.value} -> $value")
        }
        fieldSpecs.put(name, FieldSpec(type, name, value))
    }

    fun toPlatformConfig(): PlatformConfig {
        val config = PlatformConfig(name)
        config.fieldSpecs.putAll(this.fieldSpecs)

        return config
    }
}
