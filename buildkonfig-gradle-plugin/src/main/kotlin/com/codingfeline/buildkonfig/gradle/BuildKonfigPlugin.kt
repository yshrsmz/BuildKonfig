package com.codingfeline.buildkonfig.gradle


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import java.io.File

open class BuildKonfigPlugin : Plugin<Project> {


    override fun apply(target: Project) {
        val objectFactory = target.objects

        val outputDirectory = File(target.buildDir, "buildKonfig")

        val extension = target.extensions.create("buildKonfig", BuildKonfigExtension::class.java)

        val logger = Logging.getLogger(BuildKonfigPlugin::class.java)

//        val defaultConfig = instantiator.newInstance(PlatformConfigDsl, "defaults")
//        println(defaultConfig)

        extension.defaultConfigs = objectFactory.newInstance(PlatformConfigDsl::class.java, "defaults", logger)
        extension.targetConfigs =
            target.container(PlatformConfigDsl::class.java, PlatformConfigFactory(objectFactory, logger))

        val task = target.tasks.create("generateBuildKonfig", BuildKonfigTask::class.java) {
            it.setExtension(extension)
            it.outputDirectory = outputDirectory
            it.group = "buildKonfig"
            it.description = "Generate BuildKonfig"
        } as BuildKonfigTask
    }
}
