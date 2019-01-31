package com.codingfeline.buildkonfig.gradle

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

open class BuildKonfigExtension {
    var packageName: String? = null
    var defaultConfigs: TargetConfigDsl? = null
    var targetConfigs: NamedDomainObjectContainer<TargetConfigDsl>? = null

    fun defaultConfigs(config: Action<TargetConfigDsl>) {
        defaultConfigs?.let { config.execute(it) }
    }

    fun targetConfigs(config: Closure<*>) {
        this.targetConfigs?.configure(config)
    }
}
