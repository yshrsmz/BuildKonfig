package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BuildKonfigPluginTest : BaseGradlePluginTest() {

    private val buildFileHeader = buildFileHeader("kotlin-multiplatform")
    private val androidBuildFileHeader =
        buildFileHeader("kotlin-multiplatform", "com.android.kotlin.multiplatform.library")

    private val buildFileKMPConfig = """
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
        |    jvmMain {
        |      dependencies {}
        |    }
        |  }
        |}
    """.trimMargin()

    @Test
    fun `Applying plugin without any Kotlin plugin throws`() {
        buildFile.writeText(
            """
            |plugins {
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
            |   }
            |}
            """.trimMargin()
        )

        projectDir.buildKonfigDir()

        val result = gradleRunner(projectDir)
            .withArguments("build", "--stacktrace")
            .buildAndFail()

        assertThat(result.output)
            .contains("BuildKonfig Gradle plugin applied in project ':' but no supported Kotlin plugin was found")
    }

    @Test
    fun `buildkonfig block without defaultConfigs warns and skips`() {
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

        projectDir.buildKonfigDir()

        val result = gradleRunner(projectDir)
            .withArguments("build", "--stacktrace")
            .build()
            .assertBuildSuccessful()

        assertThat(result.output)
            .contains("BuildKonfig: non-flavored defaultConfigs is not provided. Skipping code generation.")
    }

    @Test
    fun `buildConfigField with invalid name throws`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.sample"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'API.URL', 'http://localhost'
            |   }
            |}
            |
            |$buildFileKMPConfig
            """.trimMargin()
        )

        projectDir.buildKonfigDir()

        val result = gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .buildAndFail()

        assertThat(result.output)
            .contains("buildConfigField name 'API.URL' is not a valid Kotlin identifier")
    }

    @Test
    fun `generateBuildKonfig fails when packageName is not set`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'env', 'production'
            |   }
            |}
            |
            |$buildFileKMPConfig
            """.trimMargin()
        )

        val result = gradleRunner(projectDir)
            .withArguments("generateBuildKonfig", "--stacktrace")
            .buildAndFail()

        // Gradle's native required-input error surfaces because BuildKonfigTask.packageName
        // is `@get:Input Property<String>` and the extension never sets it.
        assertThat(result.output).contains("packageName")
        assertThat(result.output).contains("doesn't have a configured value")
    }

    @Test
    fun `Applying the plugin works fine for multiplatform project`() {
        buildFile.writeText(
            """
            |$androidBuildFileHeader
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
            |            buildConfigField 'String', 'android', '${'$'}fuga'
            |        }
            |        iosX64 {
            |            buildConfigField 'BOOLEAN', 'native', 'true'
            |        }
            |    }
            |}
            |
            |kotlin {
            |   android {
            |       compileSdk = 28
            |       minSdk = 21
            |       namespace = "com.sample"
            |   }
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
            |     androidMain {
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
            .withArguments("generateBuildKonfig", "--stacktrace")
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

        val androidResult = buildKonfigFile(buildDir, "androidMain", "com.sample")
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

        val iosResult = buildKonfigFile(buildDir, "iosX64Main", "com.sample")
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
    fun `The generate task is a dependency of multiplatform jvm target`() {
        buildFile.writeText(buildKMPAndroidScript())

        createAndroidManifest(projectDir)

        projectDir.buildKonfigDir()

        val result = gradleRunner(projectDir)
            .withArguments("compileKotlinJvm", "--stacktrace")
            .build()

        assertThat(result.output)
            .contains("generateBuildKonfig")
    }

    @Test
    fun `The generate task is a dependency of multiplatform jvm test target`() {
        buildFile.writeText(buildKMPAndroidScript())

        createAndroidManifest(projectDir)

        projectDir.buildKonfigDir()

        val result = gradleRunner(projectDir)
            .withArguments("compileTestKotlinJvm", "--stacktrace")
            .build()

        assertThat(result.output)
            .contains("generateBuildKonfig")
    }

    @Test
    fun `The generate task is a dependency of multiplatform js target`() {
        buildFile.writeText(buildKMPAndroidScript())

        createAndroidManifest(projectDir)

        projectDir.buildKonfigDir()

        val result = gradleRunner(projectDir)
            .withArguments("compileKotlinJs", "--stacktrace")
            .build()

        assertThat(result.output)
            .contains("generateBuildKonfig")
    }

    private fun buildKMPAndroidScript(): String =
        """
        |$androidBuildFileHeader
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
        |            buildConfigField 'String', 'android', '${'$'}fuga'
        |        }
        |        iosX64 {
        |            buildConfigField 'BOOLEAN', 'native', 'true'
        |        }
        |    }
        |}
        |
        |kotlin {
        |   android {
        |       compileSdk = 28
        |       minSdk = 21
        |       namespace = "com.sample"
        |   }
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
        |     androidMain {
        |       dependencies {}
        |     }
        |     jvmMain {
        |       dependencies {}
        |     }
        |   }
        |}
        """.trimMargin()
}
