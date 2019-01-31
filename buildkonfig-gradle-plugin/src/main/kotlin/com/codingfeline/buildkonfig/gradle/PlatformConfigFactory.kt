package com.codingfeline.buildkonfig.gradle

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory

class PlatformConfigFactory(
    val objectFactory: ObjectFactory,
    val logger: Logger
) : NamedDomainObjectFactory<TargetConfigDsl> {

    override fun create(name: String): TargetConfigDsl {
        return objectFactory.newInstance(TargetConfigDsl::class.java, name, logger)
    }
}
