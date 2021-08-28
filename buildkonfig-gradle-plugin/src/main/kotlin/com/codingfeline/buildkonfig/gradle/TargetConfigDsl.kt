package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.compiler.TargetConfig
import org.gradle.api.Project
import java.io.Serializable
import javax.inject.Inject

open class TargetConfigDsl @Inject constructor(
    name: String,
    project: Project,
) : TargetConfig(name), Serializable {

    private val logger = project.logger

    companion object {
        const val serialVersionUID = 1L
    }

    @Suppress("unused")
    fun buildConfigField(
        type: FieldSpec.Type,
        name: String,
        value: String
    ) {

        val alreadyPresent = fieldSpecs[name]

        if (alreadyPresent != null) {
            logger.info("TargetConfig: buildConfigField '$name' is being replaced: ${alreadyPresent.value} -> $value")
        }
        fieldSpecs[name] = FieldSpec(type, name, value)
    }

    @Suppress("unused")
    fun buildConfigNullableField(
        type: FieldSpec.Type,
        name: String,
        value: String?
    ) {

        val alreadyPresent = fieldSpecs[name]

        if (alreadyPresent != null) {
            logger.info("TargetConfig: buildConfigField '$name' is being replaced: ${alreadyPresent.value} -> $value")
        }
        fieldSpecs[name] = FieldSpec(type, name, value, nullable = true)
    }

    fun toTargetConfig(): TargetConfig {
        return TargetConfig(name)
            .also {
                it.flavor = flavor
                it.fieldSpecs.putAll(fieldSpecs)
            }
    }

    internal fun registerTask() {

    }
}
