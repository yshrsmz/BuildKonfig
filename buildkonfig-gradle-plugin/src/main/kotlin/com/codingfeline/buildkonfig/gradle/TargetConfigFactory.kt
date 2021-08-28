package com.codingfeline.buildkonfig.gradle

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory

class TargetConfigFactory(
    private val objectFactory: ObjectFactory,
    private val project: Project
) : NamedDomainObjectFactory<TargetConfigDsl> {

    override fun create(name: String): TargetConfigDsl {
        return objectFactory.newInstance(TargetConfigDsl::class.java, name, project)
    }
}
