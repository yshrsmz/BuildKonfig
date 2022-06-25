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

    @Deprecated("Use buildConfigField instead")
    @Suppress("unused")
    fun buildConfigNullableField(
        type: FieldSpec.Type,
        name: String,
        value: String?
    ) = registerField(FieldSpec(type, name, value, nullable = true))

    @Deprecated("Use buildConfigField instead")
    @Suppress("unused")
    fun buildConfigConstField(
        type: FieldSpec.Type,
        name: String,
        value: String
    ) = registerField(FieldSpec(type, name, value, const = true))

    @Deprecated("Use buildConfigField instead")
    @Suppress("unused")
    fun buildConfigConstNullableField(
        type: FieldSpec.Type,
        name: String,
        value: String
    ) = registerField(FieldSpec(type, name, value, nullable = true, const = true))

    /**
     * Add new field
     *
     * This method is for groovy.
     *
     * The following options are available:
     * - nullable: pass true to make the field nullable. Defaults to false.
     * - const: pass true to declare the field as `const`. Defaults to false
     */
    @Suppress("unused")
    fun buildConfigField(
        args: Map<String, Any>,
        type: FieldSpec.Type,
        name: String,
        value: String?
    ) {
        val nullable = args["nullable"] as? Boolean ?: false
        val const = args["const"] as? Boolean ?: false

        buildConfigField(type = type, name = name, value = value, nullable = nullable, const = const)
    }

    @Suppress("unused")
    fun buildConfigField(
        type: FieldSpec.Type,
        name: String,
        value: String?,
        nullable: Boolean = false,
        const: Boolean = false
    ) {
        if (value == null && !nullable) {
            throw IllegalArgumentException("value is required for the non-nullable field")
        }

        registerField(FieldSpec(type = type, name = name, value = value, nullable = nullable, const = const))
    }

    fun toTargetConfig(): TargetConfig {
        return TargetConfig(name)
            .also {
                it.flavor = flavor
                it.fieldSpecs.putAll(fieldSpecs)
            }
    }
}
