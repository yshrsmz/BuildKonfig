package com.codingfeline.buildkonfig.gradle


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging

class BuildKonfigPlugin implements Plugin<Project> {

    @Override
    void apply(Project target) {
        def objectFactory = target.getObjects()

        File outputDirectory = new File(target.buildDir, 'buildKonfig')

        def extension = target.extensions.create("buildKonfig", BuildKonfigExtension.class)

        def logger = Logging.getLogger(BuildKonfigPlugin.class)

//        def defaultConfig = instantiator.newInstance(PlatformConfigDsl, "defaults")
//        println(defaultConfig)

        extension.defaultConfigs = objectFactory.newInstance(PlatformConfigDsl.class, "defaults", logger)
        extension.targetConfigs = target.container(PlatformConfigDsl.class, new PlatformConfigFactory(objectFactory, logger))

        def task = target.tasks.create("generateBuildKonfig", BuildKonfigTask.class) {
            it.setExtension(extension)
            it.outputDirectory = outputDirectory
            it.group = 'buildKonfig'
            it.description = 'Generate BuildKonfig'
        } as BuildKonfigTask
    }
}
