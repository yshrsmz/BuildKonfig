package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BuildKonfigPluginTest {

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

    private val buildFileMPPConfig = """
        |kotlin {
        |  jvm()
        |  js {
        |    browser()
        |    nodejs()
        |  }
        |  iosX64('ios')
        |
        |  sourceSets {
        |    commonMain {
        |      dependencies {}
        |    }
        |    jvmMain {
        |      dependencies {}
        |    }
        |  }
        |}
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
                        kotlin.mpp.androidSourceSetLayoutVersion=2
                        kotlin.js.compiler=ir
                        """.trimMargin()
                )
            }
    }

    @Test
    fun `Applying plugin with kotlin js plugin throws`() {
        buildFile.writeText(
            """
            |plugins {
            |   id 'org.jetbrains.kotlin.js'
            |   id 'com.codingfeline.buildkonfig'
            |}
            |
            |repositories {
            |   mavenCentral()
            |}
            |
            |buildkonfig {
            |   packageName = "com.sample"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'test', 'hoge'
            |       buildConfigField 'INT', 'intValue', '10'
            |   }
            |
            |   targetConfigs {
            |       jvm {
            |           buildConfigField 'STRING', 'test', 'jvm'
            |           buildConfigField 'STRING', 'jmv', 'jvmHoge'
            |       }
            |       customAndroid {
            |           buildConfigField 'String', 'android', '${'$'}fuga'
            |       }
            |       iosX64 {
            |           buildConfigField 'String', 'native', 'fuge'
            |       }
            |   }
            |}
            |
            |kotlin {}
            """.trimMargin()
        )

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("build", "--stacktrace")
            .buildAndFail()

        assertThat(result.output)
            .contains("BuildKonfig Gradle plugin applied in project ':' but no supported Kotlin plugin was found.")
    }

    @Test
    fun `buildkonfig block without defaultConfigs throws`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.sample"
            |
            |   targetConfigs {
            |       jvm {
            |           buildConfigField 'STRING', 'test', 'jvm'
            |           buildConfigField 'STRING', 'jmv', 'jvmHoge'
            |       }
            |       iosX64 {
            |           buildConfigField 'String', 'native', 'fuge'
            |       }
            |   }
            |}
            |
            |kotlin {
            |  jvm()
            |  js {
            |    browser()
            |    nodejs()
            |  }
            |  iosX64()
            |
            |  sourceSets {
            |    commonMain {
            |      dependencies {}
            |    }
            |  }
            |}
            """.trimMargin()
        )

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("build", "--stacktrace")
            .buildAndFail()

        assertThat(result.output)
            .contains("non-flavored defaultConfigs must be provided")
    }

    @Test
    fun `Applying the plugin works fine for multiplatform project`() {
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
            |   android('customAndroid')
            |   jvm()
            |   js(IR) {
            |    browser()
            |    nodejs()
            |   }
            |   iosX64()
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
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()

        assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val jvmResult = File(buildDir, "jvmMain/com/sample/BuildKonfig.kt")
        assertThat(jvmResult.readText())
            .apply {
                contains("actual val intValue: Int = 10")
                contains("actual val test: String = \"jvm\"")
                contains("val jvm: String = \"jvmHoge\"")
                doesNotContain("actual val jvm")
                doesNotContain("android")
                doesNotContain("native")
            }

        val androidResult = File(buildDir, "customAndroidMain/com/sample/BuildKonfig.kt")
        assertThat(androidResult.readText())
            .apply {
                contains("actual val intValue: Int = 10")
                contains("actual val test: String = \"hoge\"")
                contains("val android: String = \"${'$'}{'$'}fuga\"")
                doesNotContain("actual val android")
                doesNotContain("jvm")
                doesNotContain("native")
            }

        val jsResult = File(buildDir, "jsMain/com/sample/BuildKonfig.kt")
        assertThat(jsResult.readText())
            .apply {
                contains("actual val intValue: Int = 10")
                contains("actual val test: String = \"hoge\"")
                doesNotContain("android")
                doesNotContain("jvm")
                doesNotContain("native")
            }

        val iosResult = File(buildDir, "iosX64Main/com/sample/BuildKonfig.kt")
        assertThat(iosResult.readText())
            .apply {
                contains("actual val intValue: Int = 10")
                contains("actual val test: String = \"hoge\"")
                contains("val native: Boolean = true")
                doesNotContain("actual val native")
                doesNotContain("android")
                doesNotContain("jvm")
            }
    }

    @Test
    fun `Applying the plugin works fine for multiplatform project with only jvm`() {
        buildFile.writeText(
            """
            |plugins {
            |   id 'kotlin-multiplatform'
            |   id 'com.codingfeline.buildkonfig'
            |}
            |
            |repositories {
            |   google()
            |   mavenCentral()
            |}
            |
            |buildkonfig {
            |    packageName = "com.sample"
            |
            |    defaultConfigs {
            |        buildConfigField 'STRING', 'test', 'jvm'
            |        buildConfigField 'INT', 'intValue', '10'
            |        buildConfigField 'STRING', 'jvm', 'jvmHoge'
            |    }
            |}
            |
            |kotlin {
            |   jvm()
            |}
            """.trimMargin()
        )

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()

        assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val commonResult = File(buildDir, "commonMain/com/sample/BuildKonfig.kt")
        assertThat(commonResult.readText())
            .apply {
                contains("val intValue: Int = 10")
                contains("val test: String = \"jvm\"")
                contains("val jvm: String = \"jvmHoge\"")
                doesNotContain("actual val jvm")

                doesNotContain("val android: String = \"androidHoge\"")
                doesNotContain("actual val android")
            }

        val jvmResult = File(buildDir, "jvmMain/com/sample/BuildKonfig.kt")
        assertWithMessage("Shouldn't exist: %s", jvmResult)
            .that(jvmResult.exists())
            .isFalse()
    }

    @Test
    fun `Applying the plugin works fine for kotlin jvm project`() {
        `Applying the plugin works fine for single-target kotlin project`(
            plugins = """
            |plugins {
            |   id 'org.jetbrains.kotlin.jvm'
            |   id 'com.codingfeline.buildkonfig'
            |}
            """.trimMargin(),
            extraSetup = "",
        )
    }

    @Test
    fun `Applying the plugin works fine for kotlin android project`() {
        // Special initial setup for android
        createAndroidManifest(projectDir)

        `Applying the plugin works fine for single-target kotlin project`(
            plugins = """
            |plugins {
            |   id 'org.jetbrains.kotlin.android'
            |   id 'com.android.library'
            |   id 'com.codingfeline.buildkonfig'
            |}
            """.trimMargin(),
            extraSetup = """
            |android {
            |    namespace = "com.sample"
            |    compileSdk = 28
            |}
            """.trimMargin(),
        )
    }

    private fun `Applying the plugin works fine for single-target kotlin project`(
        plugins: String,
        extraSetup: String,
    ) {
        buildFile.writeText(
            """
            |$plugins
            |
            |repositories {
            |   google()
            |   mavenCentral()
            |}
            |
            |$extraSetup
            |
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
            |        android {
            |            buildConfigField 'STRING', 'test', 'android'
            |            buildConfigField 'STRING', 'android', 'androidHoge'
            |        }
            |    }
            |}
            """.trimMargin()
        )

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()

        assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val mainResult = File(buildDir, "main/com/sample/BuildKonfig.kt")
        assertThat(mainResult.readText())
            .apply {
                contains("val intValue: Int = 10")
                contains("val test: String = \"hoge\"")
                doesNotContain("expect val test")

                doesNotContain("val jvm: String = \"jvmHoge\"")
                doesNotContain("actual val jvm")

                doesNotContain("val android: String = \"androidHoge\"")
                doesNotContain("actual val android")
            }
    }

    @Test
    fun `The generate task is a dependency of multiplatform jvm target`() {

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
            |   android('customAndroid')
            |   jvm()
            |   js {
            |    browser()
            |    nodejs()
            |   }
            |   iosX64()
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
            .withArguments("compileKotlinJvm", "--stacktrace")
            .build()

        assertThat(result.output)
            .contains("generateBuildKonfig")
    }

    @Test
    fun `The generate task is a dependency of multiplatform jvm test target`() {
        buildFile.writeText(
            """
            |plugins {
            |   id 'kotlin-multiplatform'
            |   id 'com.android.library'
            |   id 'com.codingfeline.buildkonfig'
            |}
            |
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
            |   android('customAndroid')
            |   jvm()
            |   js {
            |    browser()
            |    nodejs()
            |   }
            |   iosX64()
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
            .withArguments("compileTestKotlinJvm", "--stacktrace")
            .build()

        assertThat(result.output)
            .contains("generateBuildKonfig")
    }

    @Test
    fun `The generate task is a dependency of multiplatform js target`() {
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
            |   android('customAndroid')
            |   jvm()
            |   js {
            |    browser()
            |    nodejs()
            |   }
            |   iosX64()
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
            .withArguments("compileKotlinJs", "--stacktrace")
            .build()

        assertThat(result.output)
            .contains("generateBuildKonfig")
    }

    @Test
    fun `The generate task is a dependency of kotlin jvm target`() {
        `The generate task is a dependency of the single-target kotlin compile task`(
            compileTaskName = "compileKotlin",
            plugins = """
            |plugins {
            |   id 'org.jetbrains.kotlin.jvm'
            |   id 'com.codingfeline.buildkonfig'
            |}
            """.trimMargin(),
            extraSetup = "",
        )
    }

    @Test
    fun `The generate task is a dependency of kotlin jvm test target`() {
        `The generate task is a dependency of the single-target kotlin compile task`(
            compileTaskName = "compileTestKotlin",
            plugins = """
            |plugins {
            |   id 'org.jetbrains.kotlin.jvm'
            |   id 'com.codingfeline.buildkonfig'
            |}
            """.trimMargin(),
            extraSetup = "",
        )
    }

    @Test
    fun `The generate task is a dependency of kotlin android target`() {
        val plugins = """
            |plugins {
            |   id 'org.jetbrains.kotlin.android'
            |   id 'com.android.library'
            |   id 'com.codingfeline.buildkonfig'
            |}
            """.trimMargin()

        val extraSetup = """
            |android {
            |    namespace = "com.sample"
            |    compileSdk = 28
            |
            |    compileOptions {
            |        sourceCompatibility JavaVersion.VERSION_1_8
            |        targetCompatibility JavaVersion.VERSION_1_8
            |    }
            |    kotlinOptions.jvmTarget = "1.8"
            |}
            """.trimMargin()

        `The generate task is a dependency of the single-target kotlin compile task`(
            compileTaskName = "compileDebugKotlin",
            plugins = plugins,
            extraSetup = extraSetup,
        )

        `The generate task is a dependency of the single-target kotlin compile task`(
            compileTaskName = "compileReleaseKotlin",
            plugins = plugins,
            extraSetup = extraSetup,
        )
    }

    private fun `The generate task is a dependency of the single-target kotlin compile task`(
        compileTaskName: String,
        plugins: String,
        extraSetup: String,
    ) {
        buildFile.writeText(
            """
            |$plugins
            |
            |repositories {
            |   google()
            |   mavenCentral()
            |}
            |
            |$extraSetup
            |
            |buildkonfig {
            |    packageName = "com.sample"
            |
            |    defaultConfigs {
            |        buildConfigField 'STRING', 'test', 'hoge'
            |        buildConfigField 'INT', 'intValue', '10'
            |    }
            |}
            """.trimMargin()
        )

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments(compileTaskName, "--stacktrace")
            .build()

        assertThat(result.output)
            .contains("generateBuildKonfig")
    }

}
