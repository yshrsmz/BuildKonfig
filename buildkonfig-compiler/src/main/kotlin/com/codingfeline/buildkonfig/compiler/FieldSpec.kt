package com.codingfeline.buildkonfig.compiler

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import java.io.Serializable

data class FieldSpec(
    val type: Type,
    val name: String,
    val value: String
) : Serializable {

    enum class Type(val typeName: TypeName) {
        STRING(String::class.asTypeName()) {
            override val template = "\"%L\""
        },
        INT(Int::class.asTypeName()),
        FLOAT(Float::class.asTypeName()),
        LONG(Long::class.asTypeName()),
        BOOLEAN(Boolean::class.asTypeName());

        open val template: String = "%L"
    }
}
