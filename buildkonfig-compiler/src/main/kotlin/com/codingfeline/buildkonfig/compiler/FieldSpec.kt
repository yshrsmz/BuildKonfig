package com.codingfeline.buildkonfig.compiler

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import java.io.Serializable

data class FieldSpec(
    private val type: Type,
    val name: String,
    val value: String?,
    val isTargetSpecific: Boolean = false,
    val nullable: Boolean = false,
    val const: Boolean = false,
) : Serializable {

    enum class Type(val _typeName: TypeName, val _template: String = "%L") {
        STRING(String::class.asTypeName(), "%S"),
        INT(Int::class.asTypeName()),
        FLOAT(Float::class.asTypeName()),
        LONG(Long::class.asTypeName()),
        BOOLEAN(Boolean::class.asTypeName());

        companion object {
            fun of(name: String): Type? {
                return Type.entries.firstOrNull { it.name == name }
            }
        }
    }

    val typeName: TypeName
        get() = with(type) { if (nullable) _typeName.copy(nullable = true) else _typeName.copy() }

    val template: String
        get() = with(type) { if (nullable && value == null) "%L" else _template }
}
