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

    private fun registerField(field: FieldSpec) {
        val name = field.name
        val alreadyPresent = fieldSpecs[name]

        if (alreadyPresent != null) {
            logger.info("TargetConfig: buildConfigField '$name' is being replaced: ${alreadyPresent.value} -> ${field.value}")
        }

        fieldSpecs[name] = field
    }

    @Suppress("unused")
    fun buildConfigField(
        type: FieldSpec.Type,
        name: String,
        value: String
    ) = registerField(FieldSpec(type, name, value))

    @Suppress("unused")
    fun buildConfigNullableField(
        type: FieldSpec.Type,
        name: String,
        value: String?
    ) = registerField(FieldSpec(type, name, value, nullable = true))

    @Suppress("unused")
    fun buildConfigConstField(
        type: FieldSpec.Type,
        name: String,
        value: String
    ) = registerField(FieldSpec(type, name, value, const = true))

    @Suppress("unused")
    fun buildConfigConstNullableField(
        type: FieldSpec.Type,
        name: String,
        value: String
    ) = registerField(FieldSpec(type, name, value, nullable = true, const = true))

    fun toTargetConfig(): TargetConfig {
        return TargetConfig(name)
            .also {
                it.flavor = flavor
                it.fieldSpecs.putAll(fieldSpecs)
            }
    }
}
