package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.compiler.BuildKonfigObjectProperties
import org.gradle.api.tasks.Input

data class BuildKonfigObjectPropertiesImpl(
    @Input override val exposeObject: Boolean,
    @Input override val packageName: String,
    @Input override val objectName: String,
) : BuildKonfigObjectProperties
