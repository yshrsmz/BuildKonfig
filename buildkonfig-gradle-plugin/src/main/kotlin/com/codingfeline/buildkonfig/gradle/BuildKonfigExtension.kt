package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.compiler.DEFAULT_KONFIG_OBJECT_NAME
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

open class BuildKonfigExtension(
    private val project: Project
) {

    private val configFactory = TargetConfigFactory(project.objects, project.logger)

    var packageName: String? = null
    var objectName: String = DEFAULT_KONFIG_OBJECT_NAME
    var exposeObjectWithName: String? = null

    val defaultConfigs = mutableMapOf<String, TargetConfigDsl>()
    val targetConfigs = mutableMapOf<String, NamedDomainObjectContainer<TargetConfigDsl>>()

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

    private fun createNewTargetConfig(): TargetConfigDsl {
        return project.objects.newInstance(
            TargetConfigDsl::class.java,
            "defaults",
            project.logger
        )
    }

    private fun createTargetConfigContainer(): NamedDomainObjectContainer<TargetConfigDsl> {
        return project.container(TargetConfigDsl::class.java, configFactory)
    }
}
