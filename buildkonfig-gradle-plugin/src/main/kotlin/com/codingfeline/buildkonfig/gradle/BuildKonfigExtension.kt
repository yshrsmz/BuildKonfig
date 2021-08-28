package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.compiler.DEFAULT_KONFIG_OBJECT_NAME
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

open class BuildKonfigExtension(
    private val project: Project
) {

    private val configFactory = TargetConfigFactory(project.objects, project)

    var packageName: String? = null
    var objectName: String = DEFAULT_KONFIG_OBJECT_NAME
    var exposeObjectWithName: String? = null

    // { [flavor: string]: TargetConfigDsl }
    val defaultConfigs = mutableMapOf<String, TargetConfigDsl>()

    // { [flavor: string]: { [target: string]: TargetConfigDsl }
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
            project
        )
    }

    private fun createTargetConfigContainer(): NamedDomainObjectContainer<TargetConfigDsl> {
        return project.container(TargetConfigDsl::class.java, configFactory)
    }

    private fun getTargetConfigByTarget(target: String): Map<String, TargetConfigDsl> {
        return targetConfigs.mapValues { (key, value) ->
            // key: flavor
            value.getByName(target)
        }
    }

    private fun asObjectProperties(): BuildKonfigObjectPropertiesImpl {
        val exposeObject = !exposeObjectWithName.isNullOrBlank()
        val name = if (exposeObject) exposeObjectWithName else objectName

        require(!packageName.isNullOrBlank()) { "packageName must be provided" }
        require(!name.isNullOrBlank()) { "objectName or exposeObjectWithName must be provided" }

        return BuildKonfigObjectPropertiesImpl(
            exposeObject = exposeObject,
            packageName = packageName!!,
            objectName = name,
        )
    }

    internal fun getTargetBuildKonfig(target: KotlinTarget): TargetBuildKonfig {
        check(defaultConfigs.containsKey("")) {
            "non flavored defaultConfigs must be provided"
        }

        val targets = getTargetConfigByTarget(target.targetName)
        return TargetBuildKonfig(
            project = project,
            objectProperties = asObjectProperties(),
            target = target,
            defaultConfigs = defaultConfigs,
            targetConfigs = targets
        )
    }
}
