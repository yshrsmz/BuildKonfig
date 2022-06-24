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

    /**
     * Add new field
     *
     * This method is for groovy.
     *
     * The following options are available:
     * - type: REQUIRED: type of the field. should be [String] or [FieldSpec.Type].
     * - name: REQUIRED: name of the field. should be [String].
     * - value: REQUIRED: value of the field. should be [String].
     * - nullable: pass true to make the field nullable. Defaults to false.
     * - const: pass true to declare the field as `const`. Defaults to false
     */
    @Suppress("unused")
    fun buildConfigField(args: Map<String, Any>) {
        val options = mapOf("nullable" to false, "const" to false) + args

        val type = kotlin.run {
            @Suppress("MoveVariableDeclarationIntoWhen")
            val maybeType = requireNotNull(options["type"]) { "type is required" }
            val result = when (maybeType) {
                is String -> FieldSpec.Type.of(maybeType.uppercase())
                is FieldSpec.Type -> maybeType
                else -> null
            }

            requireNotNull(result) { "type is provided, but not FieldSpec.Type or String" }
        }
        val name = requireNotNull(options["name"] as? String) { "name is required" }
        val nullable = options["nullable"] as? Boolean ?: false
        val const = options["const"] as? Boolean ?: false

        val value = kotlin.run {
            @Suppress("MoveVariableDeclarationIntoWhen")
            val maybeValue = options["value"]
            when {
                maybeValue is String -> maybeValue
                maybeValue == null && nullable -> null
                else -> throw IllegalArgumentException("value is required for the non-nullable field")
            }
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
