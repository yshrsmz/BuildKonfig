package com.codingfeline.buildkonfig.gradle

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

open class BuildKonfigExtension {
    var defaultConfigs: PlatformConfigDsl? = null
    var targetConfigs: NamedDomainObjectContainer<PlatformConfigDsl>? = null

    fun defaultConfigs(config: Action<PlatformConfigDsl>) {
        defaultConfigs?.let { config.execute(it) }
    }

    fun targetConfigs(config: Closure<*>) {
        this.targetConfigs?.configure(config)
    }
}
