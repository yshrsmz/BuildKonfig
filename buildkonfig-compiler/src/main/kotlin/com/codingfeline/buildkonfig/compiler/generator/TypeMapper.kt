package com.codingfeline.buildkonfig.compiler.generator

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName

object TypeMapper {
    fun map(typeName: String): TypeName {
        return when (typeName) {
            "String" -> String::class.asTypeName()
            "Int" -> Int::class.asTypeName()
            "Float" -> Float::class.asTypeName()
            "Boolean" -> Boolean::class.asTypeName()
            else -> throw IllegalStateException("Unknown type: $typeName")
        }
    }
}