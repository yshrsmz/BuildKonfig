package com.codingfeline.buildkonfig.compiler

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import java.io.Serializable

data class FieldSpec(
    val type: Type,
    val name: String,
    val value: String,
    val isTargetSpecific: Boolean = false
) : Serializable {

    enum class Type(val typeName: TypeName, val template: String = "%L") {
        STRING(String::class.asTypeName(), "%S"),
        INT(Int::class.asTypeName()),
        FLOAT(Float::class.asTypeName()),
        LONG(Long::class.asTypeName()),
        BOOLEAN(Boolean::class.asTypeName());
    }
}
