package com.codingfeline.buildkonfig.compiler.generator

import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.compiler.Logger
import com.codingfeline.buildkonfig.compiler.PlatformType
import com.codingfeline.buildkonfig.compiler.TargetConfigFile
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

abstract class BuildKonfigGenerator(
    val file: TargetConfigFile,
    val objectAnnotations: List<AnnotationSpec>,
    val objectModifiers: List<KModifier>,
    val propertyModifiers: List<KModifier>,
    val logger: Logger
) {
    fun generateFile(packageName: String, objectName: String): FileSpec {
        val builder = FileSpec.builder(packageName, objectName)
        builder.addType(generateType(objectName))
        return builder.build()
    }

    private fun generateType(objectName: String): TypeSpec {
        val obj = TypeSpec.objectBuilder(objectName)
            .addModifiers(*objectModifiers.toTypedArray())
            .addAnnotations(objectAnnotations)

        val props = requireNotNull(file.config).fieldSpecs.values
            .map { generateProp(it) }

        obj.addProperties(props)

        return obj.build()
    }

    abstract fun generateProp(fieldSpec: FieldSpec): PropertySpec

    companion object {
        /**
         * Generate common object
         */
        fun ofCommonObject(
            file: TargetConfigFile,
            exposeObject: Boolean,
            hasJsTarget: Boolean,
            logger: Logger
        ): BuildKonfigGenerator {
            val objectModifiers = listOf(getVisibilityModifier(exposeObject))
            val annotations = if (exposeObject && hasJsTarget) getJsObjectAnnotations() else emptyList()
            return object : BuildKonfigGenerator(
                file = file,
                objectAnnotations = annotations,
                objectModifiers = objectModifiers,
                propertyModifiers = emptyList(),
                logger = logger
            ) {
                override fun generateProp(fieldSpec: FieldSpec): PropertySpec {
                    val spec = PropertySpec.builder(fieldSpec.name, fieldSpec.typeName)
                        .initializer(fieldSpec.template, fieldSpec.value)
                        .addModifiers(*propertyModifiers.toTypedArray())
                    if (fieldSpec.const) {
                        spec.addModifiers(KModifier.CONST)
                    }
                    return spec.build()
                }
            }
        }

        /**
         * Generate common `expect` object
         */
        fun ofCommon(file: TargetConfigFile, exposeObject: Boolean, logger: Logger): BuildKonfigGenerator {
            val objectModifiers = listOf(KModifier.EXPECT, getVisibilityModifier(exposeObject))
            return object : BuildKonfigGenerator(
                file = file,
                objectAnnotations = emptyList(),
                objectModifiers = objectModifiers,
                propertyModifiers = emptyList(),
                logger = logger
            ) {
                override fun generateProp(fieldSpec: FieldSpec): PropertySpec {
                    val spec = PropertySpec.builder(fieldSpec.name, fieldSpec.typeName)
                        .addModifiers(*propertyModifiers.toTypedArray())
                    if (fieldSpec.const) {
                        spec.addModifiers(KModifier.CONST)
                            .addAnnotation(AnnotationSpec.builder(ClassName("kotlin", "Suppress"))
                            .addMember("%S", "CONST_VAL_WITHOUT_INITIALIZER")
                            .build())
                    }
                    return spec.build()
                }
            }
        }

        /**
         * Generate target `actual` object
         */
        fun ofTarget(file: TargetConfigFile, exposeObject: Boolean, logger: Logger): BuildKonfigGenerator {
            val objectModifiers = listOf(KModifier.ACTUAL, getVisibilityModifier(exposeObject))
            val annotations = if (exposeObject && file.targetName.platformType == PlatformType.js) {
                getJsObjectAnnotations()
            } else {
                emptyList()
            }
            return object : BuildKonfigGenerator(
                file = file,
                objectAnnotations = annotations,
                objectModifiers = objectModifiers,
                propertyModifiers = listOf(KModifier.ACTUAL),
                logger = logger
            ) {
                override fun generateProp(fieldSpec: FieldSpec): PropertySpec {
                    val spec = PropertySpec.builder(fieldSpec.name, fieldSpec.typeName)
                        .initializer(fieldSpec.template, fieldSpec.value)

                    if (!fieldSpec.isTargetSpecific) {
                        spec.addModifiers(*propertyModifiers.toTypedArray())
                    }

                    if (fieldSpec.const) {
                        spec.addModifiers(KModifier.CONST)
                    }

                    return spec.build()
                }
            }
        }
    }
}

private fun getVisibilityModifier(exposeObject: Boolean): KModifier =
    if (exposeObject) KModifier.PUBLIC else KModifier.INTERNAL

private fun getJsObjectAnnotations(): List<AnnotationSpec> {
    return listOf(
        AnnotationSpec.builder(ClassName("kotlin.js", "JsExport")).build(),
        AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
            .addMember("%T::class", ClassName("kotlin.js", "ExperimentalJsExport"))
            .build()
    )
}