package com.codingfeline.buildkonfig.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

class BuildKonfigExtension {
    PlatformConfigDsl defaultConfigs = null
    NamedDomainObjectContainer<PlatformConfigDsl> targetConfigs = null

    def defaultConfigs(Action<PlatformConfigDsl> config) {
        config.execute(defaultConfigs)
    }

    def targetConfigs(Closure config) {
        this.targetConfigs.configure(config)
    }
}
