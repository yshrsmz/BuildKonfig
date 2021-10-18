package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BuildKonfigPluginHMPPTest {

    @get:Rule
    val projectDir = TemporaryFolder()

    lateinit var buildFile: File

    lateinit var settingFile: File

    private val buildFileHeader = """
        |plugins {
        |    id 'kotlin-multiplatform'
        |    id 'com.codingfeline.buildkonfig'
        |}
        |
        |repositories {
        |   mavenCentral()
        |}
        |
    """.trimMargin()

    @Before
    fun setup() {
        buildFile = projectDir.newFile("build.gradle")
        settingFile = projectDir.newFile("settings.gradle")
        settingFile.writeText(settingsGradle)

        projectDir.newFile("gradle.properties")
            .also {
                it.writeText(
                    """
                        kotlin.mpp.stability.nowarn=true
                        kotlin.mpp.enableGranularSourceSetsMetadata=true
                        kotlin.native.enableDependencyPropagation=false
                        """.trimMargin()
                )
            }

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
            |   android('customAndroid')
            |   jvm()
            |   js {
            |    browser()
            |    nodejs()
            |   }
            |   ios()
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

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace", "--info")
            .build()

        println("result: ${result.output}")

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val jvmResult = File(buildDir, "jvmMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(jvmResult.readText())
            .apply {
                contains("actual val intValue: Int = 10")
                contains("actual val test: String = \"jvm\"")
                contains("val jvm: String = \"jvmHoge\"")
                doesNotContain("actual val jvm")
                doesNotContain("android")
                doesNotContain("native")
            }

        val androidResult = File(buildDir, "customAndroidMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(androidResult.readText())
            .apply {
                contains("actual val intValue: Int = 10")
                contains("actual val test: String = \"hoge\"")
                contains("val android: String = \"${'$'}{'$'}fuga\"")
                doesNotContain("actual val android")
                doesNotContain("jvm")
                doesNotContain("native")
            }

        val jsResult = File(buildDir, "jsMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(jsResult.readText())
            .apply {
                contains("actual val intValue: Int = 10")
                contains("actual val test: String = \"hoge\"")
                doesNotContain("android")
                doesNotContain("jvm")
                doesNotContain("native")
            }

        val iosX64Result = File(buildDir, "iosX64Main/com/sample/BuildKonfig.kt")
        Truth.assertThat(iosX64Result.readText())
            .apply {
                contains("actual val intValue: Int = 10")
                contains("actual val test: String = \"hoge\"")
                contains("val native: Boolean = true")
                doesNotContain("actual val native")
                doesNotContain("android")
                doesNotContain("jvm")
            }

        val iosArm64Result = File(buildDir, "iosArm64Main/com/sample/BuildKonfig.kt")
        Truth.assertThat(iosArm64Result.readText())
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
            |    js("browser") {
            |        browser()
            |    }
            |    js("node") {
            |        nodejs()
            |    }
            |    ios()
            |    macosX64()
            |    linuxX64()
            |    mingwX64()
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
            |     browserMain {
            |       dependsOn(jsCommonMain)
            |     }
            |     nodeMain {
            |       dependsOn(jsCommonMain)
            |     }
            |   }
            |}
            """.trimMargin()
        )

        createAndroidManifest(projectDir)

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace", "--info")
            .build()

//        println("result: ${result.output}")

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val commonKonfig = File(buildDir, "commonMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(commonKonfig.exists()).isTrue()
        Truth.assertThat(commonKonfig.readText()).apply {
            contains("expect")
            doesNotContain("actual")
        }

        val appKonfig = File(buildDir, "appMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(appKonfig.exists()).isTrue()
        Truth.assertThat(appKonfig.readText()).apply {
            contains("actual")
            doesNotContain("expect")
        }

        val androidKonfig = File(buildDir, "androidMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(androidKonfig.exists()).isFalse()

        val desktopKonfig = File(buildDir, "desktopMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(desktopKonfig.exists()).isFalse()

        val macosX64Konfig = File(buildDir, "macosX64Main/com/sample/BuildKonfig.kt")
        Truth.assertThat(macosX64Konfig.exists()).isFalse()

        val linuxX64Konfig = File(buildDir, "linuxX64Main/com/sample/BuildKonfig.kt")
        Truth.assertThat(linuxX64Konfig.exists()).isFalse()

        val mingwX64Konfig = File(buildDir, "mingwX64Main/com/sample/BuildKonfig.kt")
        Truth.assertThat(mingwX64Konfig.exists()).isFalse()

        val jvmKonfig = File(buildDir, "jvmMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(jvmKonfig.exists()).isTrue()
        Truth.assertThat(jvmKonfig.readText()).apply {
            contains("actual")
            doesNotContain("expect")
        }

        val iosKonfig = File(buildDir, "iosMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(iosKonfig.exists()).isTrue()
        Truth.assertThat(iosKonfig.readText()).apply {
            contains("actual")
            doesNotContain("expect")
        }

        val iosX64Konfig = File(buildDir, "iosX64Main/com/sample/BuildKonfig.kt")
        Truth.assertThat(iosX64Konfig.exists()).isFalse()

        val iosArm64Konfig = File(buildDir, "iosArm64Main/com/sample/BuildKonfig.kt")
        Truth.assertThat(iosArm64Konfig.exists()).isFalse()

        val jsCommonKonfig = File(buildDir, "jsCommonMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(jsCommonKonfig.exists()).isTrue()
        Truth.assertThat(jsCommonKonfig.readText()).apply {
            contains("actual")
            doesNotContain("expect")
        }

        val browserKonfig = File(buildDir, "browserMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(browserKonfig.exists()).isFalse()

        val nodeKonfig = File(buildDir, "nodeMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(nodeKonfig.exists()).isFalse()
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
            |    android {}
            |    js("browser") {
            |        browser()
            |    }
            |    js("node") {
            |        nodejs()
            |    }
            |    ios()
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

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace", "--info")
            .build()

//        println("result: ${result.output}")

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        println(buildDir.listFiles())

        val appKonfig = File(buildDir, "appMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(appKonfig.exists()).isTrue()

        val androidKonfig = File(buildDir, "androidMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(androidKonfig.exists()).isFalse()

        val jvmKonfig = File(buildDir, "jvmMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(jvmKonfig.exists()).isTrue()

        val iosKonfig = File(buildDir, "iosMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(iosKonfig.exists()).isFalse()

        val iosX64Konfig = File(buildDir, "iosX64Main/com/sample/BuildKonfig.kt")
        Truth.assertThat(iosX64Konfig.exists()).isTrue()

        val iosArm64Konfig = File(buildDir, "iosArm64Main/com/sample/BuildKonfig.kt")
        Truth.assertThat(iosArm64Konfig.exists()).isTrue()

        val browserKonfig = File(buildDir, "browserMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(browserKonfig.exists()).isTrue()

        val nodeKonfig = File(buildDir, "nodeMain/com/sample/BuildKonfig.kt")
        Truth.assertThat(nodeKonfig.exists()).isTrue()
    }
}