package com.codingfeline.buildkonfig.buildsrc

object Versions {
    const val compileSdk = 30
    const val kotlin = "1.5.30"
    const val dokka = "1.5.30"
    const val jvmTarget = "1.8"
    const val benManesVersionsPlugin = "0.39.0"
    const val gradle = "7.1.1"
}

object Dependencies {
    const val androidPlugin = "com.android.tools.build:gradle:7.0.2"
    const val kotlinPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val dokkaPlugin = "org.jetbrains.dokka:dokka-gradle-plugin:${Versions.dokka}"
    const val gradleVersionsPlugin = "com.github.ben-manes:gradle-versions-plugin:${Versions.benManesVersionsPlugin}"
    const val pluginPublishPlugin = "com.gradle.publish:plugin-publish-plugin:0.16.0"
    const val mavenPublishPlugin = "com.vanniktech:gradle-maven-publish-plugin:0.14.2"

    const val kotlinPoet = "com.squareup:kotlinpoet:1.10.1"
    const val junit = "junit:junit:4.13.2"
    const val truth = "com.google.truth:truth:1.1.3"
}
