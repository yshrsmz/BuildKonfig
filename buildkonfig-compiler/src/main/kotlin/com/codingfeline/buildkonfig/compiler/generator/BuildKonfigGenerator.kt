package com.codingfeline.buildkonfig.compiler.generator

import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.compiler.KONFIG_OBJECT_NAME
import com.codingfeline.buildkonfig.compiler.Logger
import com.codingfeline.buildkonfig.compiler.TargetConfigFile
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

abstract class BuildKonfigGenerator(
    val file: TargetConfigFile,
    val objectModifiers: Array<KModifier>,
    val propertyModifiers: Array<KModifier>,
    val logger: Logger
) {

    fun generateType(): TypeSpec {
        val obj = TypeSpec.objectBuilder(KONFIG_OBJECT_NAME)
            .addModifiers(*objectModifiers)

        val props = file.config.fieldSpecs.values
            .map { generateProp(it) }

        obj.addProperties(props)

        return obj.build()
    }

    abstract fun generateProp(fieldSpec: FieldSpec): PropertySpec

    companion object {
        fun ofCommon(file: TargetConfigFile, logger: Logger): BuildKonfigGenerator {
            return object : BuildKonfigGenerator(
                file = file,
                objectModifiers = arrayOf(KModifier.EXPECT),
                propertyModifiers = emptyArray(),
                logger = logger
            ) {
                override fun generateProp(fieldSpec: FieldSpec): PropertySpec {
                    return PropertySpec.builder(fieldSpec.name, fieldSpec.type.typeName)
                        .addModifiers(*propertyModifiers)
                        .build()
                }
            }
        }

        fun ofTarget(file: TargetConfigFile, logger: Logger): BuildKonfigGenerator {
            return object : BuildKonfigGenerator(
                file = file,
                objectModifiers = arrayOf(KModifier.ACTUAL),
                propertyModifiers = arrayOf(KModifier.ACTUAL),
                logger = logger
            ) {
                override fun generateProp(fieldSpec: FieldSpec): PropertySpec {
                    return PropertySpec.builder(fieldSpec.name, fieldSpec.type.typeName)
                        .addModifiers(*propertyModifiers)
                        .initializer(fieldSpec.type.template, fieldSpec.value)
                        .build()
                }
            }
        }
    }
}