package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.compiler.DEFAULT_KONFIG_OBJECT_NAME
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

@BuildKonfigDsl
abstract class BuildKonfigExtension @Inject constructor(
    private val objects: ObjectFactory,
    private val logger: Logger,
) {

    abstract val packageName: Property<String>
    abstract val objectName: Property<String>
    abstract val exposeObjectWithName: Property<String>

    val defaultConfigs: MutableMap<String, TargetConfigDsl> = mutableMapOf()
    val targetConfigs: MutableMap<String, NamedDomainObjectContainer<TargetConfigDsl>> = mutableMapOf()

    private val configFactory = TargetConfigFactory(objects, logger)

    init {
        objectName.convention(DEFAULT_KONFIG_OBJECT_NAME)
    }

    @Suppress("unused")
    fun defaultConfigs(config: Action<TargetConfigDsl>) {
        defaultConfigs.computeIfAbsent("") { createNewTargetConfig() }
            .let {
                config.execute(it)
                it.flavor = ""
            }
    }

    @Suppress("unused")
    fun defaultConfigs(flavor: String, config: Action<TargetConfigDsl>) {
        defaultConfigs.computeIfAbsent(flavor) { createNewTargetConfig() }
            .let {
                config.execute(it)
                it.flavor = flavor
            }
    }

    @Suppress("unused")
    fun targetConfigs(config: Action<NamedDomainObjectContainer<TargetConfigDsl>>) {
        val container = targetConfigs.computeIfAbsent("") { createTargetConfigContainer() }
        config.execute(container)
        container.forEach { it.flavor = "" }
    }

    @Suppress("unused")
    fun targetConfigs(flavor: String, config: Action<NamedDomainObjectContainer<TargetConfigDsl>>) {
        val container = targetConfigs.computeIfAbsent(flavor) { createTargetConfigContainer() }
        config.execute(container)
        container.forEach { it.flavor = flavor }
    }

    private fun createNewTargetConfig(): TargetConfigDsl =
        objects.newInstance(TargetConfigDsl::class.java, "defaults", logger)

    private fun createTargetConfigContainer(): NamedDomainObjectContainer<TargetConfigDsl> =
        objects.domainObjectContainer(TargetConfigDsl::class.java, configFactory)
}
