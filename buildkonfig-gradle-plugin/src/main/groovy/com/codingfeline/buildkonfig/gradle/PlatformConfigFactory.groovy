package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.compiler.PlatformConfig
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory

class PlatformConfigFactory implements NamedDomainObjectFactory<PlatformConfig> {

    ObjectFactory objectFactory

    Logger logger

    PlatformConfigFactory(
            ObjectFactory objectFactory,
            Logger logger) {
        this.objectFactory = objectFactory
        this.logger = logger
    }

    @Override
    PlatformConfigDsl create(String name) {
        return objectFactory.newInstance(PlatformConfigDsl.class, name, logger)
    }
}
