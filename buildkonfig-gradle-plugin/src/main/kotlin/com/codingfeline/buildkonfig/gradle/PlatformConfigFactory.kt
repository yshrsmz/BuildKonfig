package com.codingfeline.buildkonfig.gradle

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory

class PlatformConfigFactory(
    val objectFactory: ObjectFactory,
    val logger: Logger
) : NamedDomainObjectFactory<PlatformConfigDsl> {

    override fun create(name: String): PlatformConfigDsl {
        return objectFactory.newInstance(PlatformConfigDsl::class.java, name, logger)
    }
}
