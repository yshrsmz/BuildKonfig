package com.codingfeline.buildkonfig.buildsrc

object Versions {
    const val compileSdk = 29
    const val kotlin = "1.3.72"
    const val dokka = "0.10.1"
    const val jvmTarget = "1.8"
    const val benManesVersionsPlugin = "0.29.0"
    const val gradle = "6.5.1"
}

object Dependencies {
    const val androidPlugin = "com.android.tools.build:gradle:4.0.1"
    const val kotlinPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val serializationPlugin = "org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}"
    const val dokkaPlugin = "org.jetbrains.dokka:dokka-gradle-plugin:${Versions.dokka}"
    const val gradleVersionsPlugin = "com.github.ben-manes:gradle-versions-plugin:${Versions.benManesVersionsPlugin}"
    const val pluginPublishPlugin = "com.gradle.publish:plugin-publish-plugin:0.12.0"

    const val kotlinPoet = "com.squareup:kotlinpoet:1.6.0"
    const val junit = "junit:junit:4.13"
    const val truth = "com.google.truth:truth:1.0.1"
}
