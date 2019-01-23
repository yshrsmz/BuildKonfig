package com.codingfeline.buildkonfig.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class BuildKonfigPlugin implements Plugin<Project>{
    @Override
    void apply(Project target) {
        def extension = target.extensions.create("buildKonfig", BuildKonfigExtension.class)
    }
}
