package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BuildKonfigPluginHMPPTest : BaseGradlePluginTest() {

    override fun extraSetup() {
        projectDir.newFile("gradle.properties").writeText(
            """
                kotlin.mpp.androidSourceSetLayoutVersion=2
                kotlin.js.compiler=ir
                """.trimMargin()
        )
    }

    @Test
    fun `Applying the plugin works fine for the hierarchical multiplatform project`() {
        buildFile.writeText(
            """
            |plugins {
            |   id 'kotlin-multiplatform'
            |   id 'com.android.library'
            |   id 'com.codingfeline.buildkonfig'
            |}
            |
            |repositories {
            |   google()
            |   mavenCentral()
            |}
            |
            |android {
            |    compileSdkVersion 28
            |
            |    defaultConfig {
            |        minSdkVersion 21
            |        targetSdkVersion 28
            |        versionCode 1
            |        versionName "1.0"
            |    }
            |    buildTypes {
            |        release {
            |            minifyEnabled false
            |            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
            |        }
            |    }
            |
            |    sourceSets {
            |        main {
            |            manifest.srcFile 'src/androidMain/AndroidManifest.xml'
            |        }
            |    }
            |
            |    namespace = "com.sample"
            |}
            |buildkonfig {
            |    packageName = "com.sample"
            |
            |    defaultConfigs {
            |        buildConfigField 'STRING', 'test', 'hoge'
            |        buildConfigField 'INT', 'intValue', '10'
            |    }
            |
            |    targetConfigs {
            |        jvm {
            |            buildConfigField 'STRING', 'test', 'jvm'
            |            buildConfigField 'STRING', 'jvm', 'jvmHoge'
            |        }
            |        customAndroid {
            |            buildConfigField 'String', 'android', '${'$'}fuga'
            |        }
            |        iosX64 {
            |            buildConfigField 'BOOLEAN', 'native', 'true'
            |        }
            |    }
            |}
            |
            |kotlin {
            |   androidTarget('customAndroid')
            |   jvm()
            |   js(IR) {
            |    browser()
            |    nodejs()
            |   }
            |   iosX64()
            |   iosArm64()
            |   iosSimulatorArm64()
            |
            |   sourceSets {
            |     commonMain {
            |       dependencies {}
            |     }
            |     customAndroidMain {
            |       dependencies {}
            |     }
            |     jvmMain {
            |       dependencies {}
            |     }
            |   }
            |}
            """.trimMargin()
        )

        createAndroidManifest(projectDir)

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace", "--info")
            .build()
            .assertBuildSuccessful()

        val jvmResult = buildKonfigFile(buildDir, "jvmMain", "com.sample")
        assertThat(jvmResult.readText())
            .apply {
                contains("actual val intValue: Int = 10")
                contains("actual val test: String = \"jvm\"")
                contains("val jvm: String = \"jvmHoge\"")
                doesNotContain("actual val jvm")
                doesNotContain("android")
                doesNotContain("native")
            }

        val androidResult = buildKonfigFile(buildDir, "customAndroidMain", "com.sample")
        assertThat(androidResult.readText())
            .apply {
                contains("actual val intValue: Int = 10")
                contains("actual val test: String = \"hoge\"")
                contains("val android: String = \"${'$'}{'$'}fuga\"")
                doesNotContain("actual val android")
                doesNotContain("jvm")
                doesNotContain("native")
            }

        val jsResult = buildKonfigFile(buildDir, "jsMain", "com.sample")
        assertThat(jsResult.readText())
            .apply {
                contains("actual val intValue: Int = 10")
                contains("actual val test: String = \"hoge\"")
                doesNotContain("android")
                doesNotContain("jvm")
                doesNotContain("native")
            }

        val iosX64Result = buildKonfigFile(buildDir, "iosX64Main", "com.sample")
        assertThat(iosX64Result.readText())
            .apply {
                contains("actual val intValue: Int = 10")
                contains("actual val test: String = \"hoge\"")
                contains("val native: Boolean = true")
                doesNotContain("actual val native")
                doesNotContain("android")
                doesNotContain("jvm")
            }

        val iosArm64Result = buildKonfigFile(buildDir, "iosArm64Main", "com.sample")
        assertThat(iosArm64Result.readText())
            .apply {
                contains("actual val intValue: Int = 10")
                contains("actual val test: String = \"hoge\"")
//                contains("val native: Boolean = true")
                doesNotContain("actual val native")
                doesNotContain("android")
                doesNotContain("jvm")
            }
    }

    @Test
    fun `Applying the plugin works fine for the complex hierarchical multiplatform project`() {
        buildFile.writeText(
            """
            |plugins {
            |   id 'kotlin-multiplatform'
            |   id 'com.android.library'
            |   id 'com.codingfeline.buildkonfig'
            |}
            |
            |repositories {
            |   google()
            |   mavenCentral()
            |}
            |
            |android {
            |    compileSdkVersion 28
            |
            |    defaultConfig {
            |        minSdkVersion 21
            |        targetSdkVersion 28
            |        versionCode 1
            |        versionName "1.0"
            |    }
            |    buildTypes {
            |        release {
            |            minifyEnabled false
            |            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
            |        }
            |    }
            |
            |    sourceSets {
            |        main {
            |            manifest.srcFile 'src/androidMain/AndroidManifest.xml'
            |        }
            |    }
            |
            |    namespace = "com.sample"
            |}
            |buildkonfig {
            |    packageName = "com.sample"
            |
            |    defaultConfigs {
            |       buildConfigField 'STRING', 'platform', 'unknown'
            |    }
            |
            |    targetConfigs {
            |       jvm {
            |           buildConfigField 'STRING', 'platform', 'jvm'
            |           buildConfigField 'STRING', 'jvm', 'jvmvalue'
            |       }
            |       app {
            |          buildConfigField 'STRING', 'platform', 'app'
            |       }
            |       android {
            |           buildConfigField 'String', 'platform', 'android'
            |           buildConfigField 'String', 'android', 'androidvalue'
            |       }
            |       apple {
            |           buildConfigField 'String', 'platform', 'apple'
            |           buildConfigField 'String', 'native', 'nativevalue'
            |       }
            |       ios {
            |           buildConfigField 'String', 'platform', 'ios'
            |           buildConfigField 'String', 'native', 'nativevalue'
            |       }
            |       jsCommon {
            |          buildConfigField 'STRING', 'platform', 'jvm'
            |       }
            |    }
            |}
            |
            |kotlin {
            |    jvm {}
            |    android {}
            |    js("jsCommon", IR) {
            |        browser()
            |        nodejs()
            |    }
            |    iosX64()
            |    iosArm64()
            |    iosSimulatorArm64()
            |    macosX64()
            |    linuxX64()
            |    mingwX64()
            |
            |    applyDefaultHierarchyTemplate()
            |
            |    sourceSets {
            |     commonMain {}
            |     androidMain {}
            |     jvmMain {}
            |
            |     appMain {
            |       dependsOn(commonMain)
            |     }
            |
            |     androidMain {
            |       dependsOn(appMain)
            |     }
            |     desktopMain {
            |       dependsOn(appMain)
            |     }
            |     macosX64Main {
            |       dependsOn(desktopMain)
            |     }
            |     linuxX64Main {
            |       dependsOn(desktopMain)
            |     }
            |     mingwX64Main {
            |       dependsOn(desktopMain)
            |     }
            |
            |     jsCommonMain {
            |       dependsOn(commonMain)
            |     }
            |   }
            |}
            """.trimMargin()
        )

        createAndroidManifest(projectDir)

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace", "--info")
            .build()
            .assertBuildSuccessful()

        val commonKonfig = buildKonfigFile(buildDir, "commonMain", "com.sample")
        assertThat(commonKonfig.exists()).isTrue()
        assertThat(commonKonfig.readText()).apply {
            contains("expect")
            doesNotContain("actual")
        }

        val appKonfig = buildKonfigFile(buildDir, "appMain", "com.sample")
        assertThat(appKonfig.exists()).isTrue()
        assertThat(appKonfig.readText()).apply {
            contains("actual")
            doesNotContain("expect")

            contains("val platform: String = \"app\"")
        }

        assertThat(buildKonfigFile(buildDir, "androidMain", "com.sample").exists()).isFalse()
        assertThat(buildKonfigFile(buildDir, "desktopMain", "com.sample").exists()).isFalse()
        assertThat(buildKonfigFile(buildDir, "macosX64Main", "com.sample").exists()).isFalse()
        assertThat(buildKonfigFile(buildDir, "linuxX64Main", "com.sample").exists()).isFalse()
        assertThat(buildKonfigFile(buildDir, "mingwX64Main", "com.sample").exists()).isFalse()

        val jvmKonfig = buildKonfigFile(buildDir, "jvmMain", "com.sample")
        assertThat(jvmKonfig.exists()).isTrue()
        assertThat(jvmKonfig.readText()).apply {
            contains("actual")
            doesNotContain("expect")
        }

        val appleKonfig = buildKonfigFile(buildDir, "appleMain", "com.sample")
        assertThat(appleKonfig.exists()).isTrue()
        assertThat(appleKonfig.readText()).apply {
            contains("actual")
            doesNotContain("expect")
        }

        val iosKonfig = buildKonfigFile(buildDir, "iosMain", "com.sample")
        assertThat(iosKonfig.exists()).isTrue()
        assertThat(iosKonfig.readText()).apply {
            contains("actual")
            doesNotContain("expect")
        }

        assertThat(buildKonfigFile(buildDir, "iosX64Main", "com.sample").exists()).isFalse()
        assertThat(buildKonfigFile(buildDir, "iosArm64Main", "com.sample").exists()).isFalse()

        val jsCommonKonfig = buildKonfigFile(buildDir, "jsCommonMain", "com.sample")
        assertThat(jsCommonKonfig.exists()).isTrue()
        assertThat(jsCommonKonfig.readText()).apply {
            contains("actual")
            doesNotContain("expect")
        }

        assertThat(buildKonfigFile(buildDir, "browserMain", "com.sample").exists()).isFalse()
        assertThat(buildKonfigFile(buildDir, "nodeMain", "com.sample").exists()).isFalse()
    }

    @Test
    fun `warns when a leaf target config is dominated by an intermediate source set with its own config`() {
        buildFile.writeText(
            """
            |${buildFileHeader("kotlin-multiplatform")}
            |
            |buildkonfig {
            |    packageName = "com.sample"
            |
            |    defaultConfigs {
            |       buildConfigField 'STRING', 'platform', 'unknown'
            |    }
            |
            |    targetConfigs {
            |       app {
            |          buildConfigField 'STRING', 'platform', 'app'
            |       }
            |       jvm {
            |           buildConfigField 'STRING', 'platform', 'jvm'
            |       }
            |    }
            |}
            |
            |kotlin {
            |    jvm()
            |    iosX64()
            |
            |    sourceSets {
            |     commonMain {}
            |     appMain {
            |       dependsOn(commonMain)
            |     }
            |     jvmMain {
            |       dependsOn(appMain)
            |     }
            |     iosX64Main {
            |       dependsOn(appMain)
            |     }
            |   }
            |}
            """.trimMargin()
        )

        val buildDir = projectDir.buildKonfigDir()

        val result = gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        // The leaf jvmMain config is dropped because its intermediate parent appMain
        // already has a config. The warning surfaces both source set names so a future
        // accidental rewrite can't silently swap them.
        assertThat(result.output)
            .contains("BuildKonfig configuration for SourceSet(jvmMain) is ignored")
        assertThat(result.output).contains("appMain")

        // The parent appMain config wins for the JVM compilation.
        val appKonfig = buildKonfigFile(buildDir, "appMain", "com.sample")
        assertThat(appKonfig.exists()).isTrue()
        assertThat(appKonfig.readText()).contains("val platform: String = \"app\"")
        assertThat(buildKonfigFile(buildDir, "jvmMain", "com.sample").exists()).isFalse()
    }

    @Test
    fun `Works fine for non-shared intermediate SourceSet`() {
        buildFile.writeText(
            """
            |plugins {
            |   id 'kotlin-multiplatform'
            |   id 'com.android.library'
            |   id 'com.codingfeline.buildkonfig'
            |}
            |
            |repositories {
            |   google()
            |   mavenCentral()
            |}
            |
            |android {
            |    compileSdkVersion 28
            |
            |    defaultConfig {
            |        minSdkVersion 21
            |        targetSdkVersion 28
            |        versionCode 1
            |        versionName "1.0"
            |    }
            |    buildTypes {
            |        release {
            |            minifyEnabled false
            |            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
            |        }
            |    }
            |
            |    sourceSets {
            |        main {
            |            manifest.srcFile 'src/androidMain/AndroidManifest.xml'
            |        }
            |    }
            |
            |    namespace = "com.sample"
            |}
            |buildkonfig {
            |    packageName = "com.sample"
            |
            |    defaultConfigs {
            |       buildConfigField 'STRING', 'platform', 'unknown'
            |    }
            |
            |    targetConfigs {
            |       app {
            |          buildConfigField 'STRING', 'platform', 'app'
            |          buildConfigField 'STRING', 'app', 'appvalue'
            |       }
            |       android {
            |           buildConfigField 'String', 'platform', 'android'
            |           buildConfigField 'String', 'android', 'androidvalue'
            |       }
            |    }
            |}
            |
            |kotlin {
            |    jvm {}
            |    androidTarget {}
            |    js("browser") {
            |        browser()
            |    }
            |    js("node") {
            |        nodejs()
            |    }
            |    iosX64()
            |    iosArm64()
            |
            |    sourceSets {
            |     commonMain {}
            |     androidMain {}
            |
            |     appMain {
            |       dependsOn(commonMain)
            |     }
            |
            |     androidMain {
            |       dependsOn(appMain)
            |     }
            |   }
            |}
            """.trimMargin()
        )

        createAndroidManifest(projectDir)

        val buildDir = projectDir.buildKonfigDir()

        gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace", "--info")
            .build()
            .assertBuildSuccessful()

        val appKonfig = buildKonfigFile(buildDir, "appMain", "com.sample")
        assertThat(appKonfig.exists()).isTrue()
        assertThat(appKonfig.readText()).apply {
            contains("actual")
            doesNotContain("expect")

            contains("val platform: String = \"app\"")
            contains("val app: String = \"appvalue\"")
        }

        assertThat(buildKonfigFile(buildDir, "androidMain", "com.sample").exists()).isFalse()
        assertThat(buildKonfigFile(buildDir, "jvmMain", "com.sample").exists()).isTrue()
        assertThat(buildKonfigFile(buildDir, "iosMain", "com.sample").exists()).isFalse()
        assertThat(buildKonfigFile(buildDir, "iosX64Main", "com.sample").exists()).isTrue()
        assertThat(buildKonfigFile(buildDir, "iosArm64Main", "com.sample").exists()).isTrue()
        assertThat(buildKonfigFile(buildDir, "browserMain", "com.sample").exists()).isTrue()
        assertThat(buildKonfigFile(buildDir, "nodeMain", "com.sample").exists()).isTrue()
    }
}
