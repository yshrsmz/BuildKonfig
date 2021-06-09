package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
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
    }

    @Test
    fun `Applying plugin with kotlin jvm plugin throws`() {
        buildFile.writeText(
            """
            |plugins {
            |   id 'org.jetbrains.kotlin.jvm'
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
            .contains("BuildKonfig Gradle plugin applied in project ':' but no supported Kotlin multiplatform plugin was found")
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
            .contains("non flavored defaultConfigs must be provided")
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

        val iosX64Result = File(buildDir, "iosX64Main/com/sample/BuildKonfig.kt")
        assertThat(iosX64Result.readText())
            .apply {
                contains("actual val intValue: Int = 10")
                contains("actual val test: String = \"hoge\"")
                contains("val native: Boolean = true")
                doesNotContain("actual val native")
                doesNotContain("android")
                doesNotContain("jvm")
            }

        val iosArm64Result = File(buildDir, "iosArm64Main/com/sample/BuildKonfig.kt")
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


}
