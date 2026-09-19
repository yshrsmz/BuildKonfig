package com.codingfeline.buildkonfig.compiler.generator

import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.compiler.BuildKonfigLogger
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
    val logger: BuildKonfigLogger,
    val fileSuppressions: List<String> = listOf(REDUNDANT_VISIBILITY_MODIFIER)
) {
    fun generateFile(packageName: String, objectName: String): FileSpec {
        val builder = FileSpec.builder(packageName, objectName)
        builder.addAnnotation(suppressAnnotation(fileSuppressions))
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
         * KotlinPoet always emits an explicit `public` modifier, which the Kotlin compiler reports
         * as a warning once `extraWarnings` is enabled.
         */
        private const val REDUNDANT_VISIBILITY_MODIFIER = "REDUNDANT_VISIBILITY_MODIFIER"

        /**
         * `expect`/`actual` objects are still in Beta and warned about on every use.
         */
        private const val EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA =
            "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"

        /**
         * Generate common object
         */
        fun ofCommonObject(
            file: TargetConfigFile,
            exposeObject: Boolean,
            hasJsTarget: Boolean,
            logger: BuildKonfigLogger
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
         * Generate common `expect` object.
         *
         * `const` is intentionally omitted on the expect side: starting with the K2 compiler
         * (Kotlin 2.x), `expect const val` without an initializer is a hard compile error and
         * the previous `@Suppress("CONST_VAL_WITHOUT_INITIALIZER")` workaround no longer
         * applies. `expect val` paired with `actual const val` is a supported pattern.
         */
        fun ofCommon(file: TargetConfigFile, exposeObject: Boolean, logger: BuildKonfigLogger): BuildKonfigGenerator {
            val objectModifiers = listOf(KModifier.EXPECT, getVisibilityModifier(exposeObject))
            return object : BuildKonfigGenerator(
                file = file,
                objectAnnotations = emptyList(),
                objectModifiers = objectModifiers,
                propertyModifiers = emptyList(),
                logger = logger,
                fileSuppressions = listOf(REDUNDANT_VISIBILITY_MODIFIER, EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA)
            ) {
                override fun generateProp(fieldSpec: FieldSpec): PropertySpec {
                    return PropertySpec.builder(fieldSpec.name, fieldSpec.typeName)
                        .addModifiers(*propertyModifiers.toTypedArray())
                        .build()
                }
            }
        }

        /**
         * Generate target `actual` object
         */
        fun ofTarget(file: TargetConfigFile, exposeObject: Boolean, logger: BuildKonfigLogger): BuildKonfigGenerator {
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
                logger = logger,
                fileSuppressions = listOf(REDUNDANT_VISIBILITY_MODIFIER, EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA)
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

/**
 * Generated code is not meant to be hand-edited, so warnings it inevitably triggers are suppressed
 * at the file level. Without this, projects using `allWarningsAsErrors` fail to compile.
 */
private fun suppressAnnotation(names: List<String>): AnnotationSpec =
    AnnotationSpec.builder(ClassName("kotlin", "Suppress"))
        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
        .apply { names.forEach { addMember("%S", it) } }
        .build()

private fun getJsObjectAnnotations(): List<AnnotationSpec> {
    return listOf(
        AnnotationSpec.builder(ClassName("kotlin.js", "JsExport")).build(),
        AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
            .addMember("%T::class", ClassName("kotlin.js", "ExperimentalJsExport"))
            .build()
    )
}