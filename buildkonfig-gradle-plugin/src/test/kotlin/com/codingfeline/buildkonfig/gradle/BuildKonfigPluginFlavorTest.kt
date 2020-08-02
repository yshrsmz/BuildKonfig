package com.codingfeline.buildkonfig.gradle

import com.google.common.truth.Truth
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BuildKonfigPluginFlavorTest {

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
        |  js()
        |  iosX64('ios')
        |}
    """.trimMargin()

    @Before
    fun setup() {
        buildFile = projectDir.newFile("build.gradle")
        settingFile = projectDir.newFile("settings.gradle")
        settingFile.writeText(
            """
            |pluginManagement {
            |   resolutionStrategy {
            |       eachPlugin {
            |           if (requested.id.id == "kotlin-multiplatform") {
            |               useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}{requested.version}")
            |           }
            |       }
            |   }
            |
            |   repositories {
            |       mavenCentral()
            |       jcenter()
            |       maven { url 'https://plugins.gradle.org/m2/' }
            |   }
            |}
            |enableFeaturePreview("GRADLE_METADATA")
        """.trimMargin()
        )
    }

    @Test
    fun `common object should be generated if there is no targetConfigs`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'value', 'defaultValue'
            |   }
            |}
            |$buildFileMPPConfig
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

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val commonResult = File(buildDir, "commonMain/com/example/BuildKonfig.kt")
        Truth.assertThat(commonResult.readText())
            .isEqualTo(
                """
                |package com.example
                |
                |import kotlin.String
                |
                |internal object BuildKonfig {
                |  val value: String = "defaultValue"
                |}
                |
            """.trimMargin()
            )

        val jvmResult = File(buildDir, "jvmMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jvmResult.exists()).isFalse()

        val jsResult = File(buildDir, "jsMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jsResult.exists()).isFalse()

        val iosResult = File(buildDir, "iosMain/com/example/BuildKonfig.kt")
        Truth.assertThat(iosResult.exists()).isFalse()
    }

    @Test
    fun `flavor can be obtained from gradle properties file`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'value', 'defaultValue'
            |   }
            |   defaultConfigs("dev") {
            |       buildConfigField 'STRING', 'value', 'devDefaultValue'
            |   }
            |}
            |$buildFileMPPConfig
        """.trimMargin()
        )

        val propertyFile = projectDir.newFile("gradle.properties")
        propertyFile.writeText("buildkonfig.flavor=dev")

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val commonResult = File(buildDir, "commonMain/com/example/BuildKonfig.kt")
        Truth.assertThat(commonResult.readText())
            .contains("val value: String = \"devDefaultValue\"")

        val jvmResult = File(buildDir, "jvmMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jvmResult.exists()).isFalse()

        val jsResult = File(buildDir, "jsMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jsResult.exists()).isFalse()

        val iosResult = File(buildDir, "iosMain/com/example/BuildKonfig.kt")
        Truth.assertThat(iosResult.exists()).isFalse()
    }

    @Test
    fun `flavor can be overwritten by cli parameter`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'value', 'defaultValue'
            |   }
            |   defaultConfigs("dev") {
            |       buildConfigField 'STRING', 'value', 'devDefaultValue'
            |   }
            |   defaultConfigs("release") {
            |       buildConfigField 'STRING', 'value', 'releaseDefaultValue'
            |   }
            |}
            |$buildFileMPPConfig
        """.trimMargin()
        )

        val propertyFile = projectDir.newFile("gradle.properties")
        propertyFile.writeText("buildkonfig.flavor=dev")

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace", "-Pbuildkonfig.flavor=release")
            .build()

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val commonResult = File(buildDir, "commonMain/com/example/BuildKonfig.kt")
        Truth.assertThat(commonResult.readText())
            .contains("val value: String = \"releaseDefaultValue\"")

        val jvmResult = File(buildDir, "jvmMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jvmResult.exists()).isFalse()

        val jsResult = File(buildDir, "jsMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jsResult.exists()).isFalse()

        val iosResult = File(buildDir, "iosMain/com/example/BuildKonfig.kt")
        Truth.assertThat(iosResult.exists()).isFalse()
    }

    @Test
    fun `Default targetConfigs overwrite flavored defaultConfigs`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'value', 'defaultValue'
            |   }
            |   defaultConfigs("dev") {
            |       buildConfigField 'STRING', 'value', 'devDefaultValue'
            |   }
            |   targetConfigs {
            |       jvm {
            |           buildConfigField 'STRING', 'value', 'jvmDefaultValue'
            |       }
            |   }
            |}
            |$buildFileMPPConfig
        """.trimMargin()
        )

        val propertyFile = projectDir.newFile("gradle.properties")
        propertyFile.writeText("buildkonfig.flavor=dev")

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val jvmResult = File(buildDir, "jvmMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jvmResult.readText())
            .contains("jvmDefaultValue")

        val jsResult = File(buildDir, "jsMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jsResult.readText())
            .contains("devDefaultValue")

        val iosResult = File(buildDir, "iosMain/com/example/BuildKonfig.kt")
        Truth.assertThat(iosResult.readText())
            .contains("devDefaultValue")
    }

    @Test
    fun `Flavored targetConfigs overwrite flavored defaultConfigs`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'value', 'defaultValue'
            |   }
            |   defaultConfigs("dev") {
            |       buildConfigField 'STRING', 'value', 'devDefaultValue'
            |   }
            |   targetConfigs("dev") {
            |       js {
            |           buildConfigField 'STRING', 'value', 'devJsValue'
            |       }
            |   }
            |}
            |$buildFileMPPConfig
        """.trimMargin()
        )

        val propertyFile = projectDir.newFile("gradle.properties")
        propertyFile.writeText("buildkonfig.flavor=dev")

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val jvmResult = File(buildDir, "jvmMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jvmResult.readText())
            .contains("devDefaultValue")

        val jsResult = File(buildDir, "jsMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jsResult.readText())
            .contains("devJsValue")

        val iosResult = File(buildDir, "iosMain/com/example/BuildKonfig.kt")
        Truth.assertThat(iosResult.readText())
            .contains("devDefaultValue")
    }

    @Test
    fun `Can create nullable field`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigNullableField 'STRING', 'value', 'defaultValue'
            |       buildConfigNullableField 'INT', 'intValue', '10'
            |   }
            |}
            |$buildFileMPPConfig
        """.trimMargin()
        )

        val propertyFile = projectDir.newFile("gradle.properties")
        propertyFile.writeText("buildkonfig.flavor=dev")

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val commonResult = File(buildDir, "commonMain/com/example/BuildKonfig.kt")
        Truth.assertThat(commonResult.readText()).apply {
            contains("String?")
            contains("defaultValue")
            contains("intValue: Int? = 10")
        }
    }

    @Test
    fun `Can assign null to nullable field`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigNullableField 'STRING', 'value', null
            |       buildConfigNullableField 'INT', 'intValue', null
            |   }
            |}
            |$buildFileMPPConfig
        """.trimMargin()
        )

        val propertyFile = projectDir.newFile("gradle.properties")
        propertyFile.writeText("buildkonfig.flavor=dev")

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val commonResult = File(buildDir, "commonMain/com/example/BuildKonfig.kt")
        Truth.assertThat(commonResult.readText()).apply {
            contains("value: String? = null")
            contains("intValue: Int? = null")
        }
    }

    @Test
    fun `Flavored targetConfigs overwrite default targetConfigs`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'value', 'defaultValue'
            |   }
            |   targetConfigs {
            |       js {
            |           buildConfigField 'STRING', 'value', 'defaultJsValue'
            |       }
            |   }
            |   targetConfigs("dev") {
            |       js {
            |           buildConfigField 'STRING', 'value', 'devJsValue'
            |       }
            |   }
            |}
            |$buildFileMPPConfig
        """.trimMargin()
        )

        val propertyFile = projectDir.newFile("gradle.properties")
        propertyFile.writeText("buildkonfig.flavor=dev")

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val jvmResult = File(buildDir, "jvmMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jvmResult.readText())
            .contains("defaultValue")

        val jsResult = File(buildDir, "jsMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jsResult.readText())
            .contains("devJsValue")

        val iosResult = File(buildDir, "iosMain/com/example/BuildKonfig.kt")
        Truth.assertThat(iosResult.readText())
            .contains("defaultValue")
    }

    @Test
    fun `Passing non-existent flavor results in default configurations`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'value', 'defaultValue'
            |   }
            |   targetConfigs {
            |       js {
            |           buildConfigField 'STRING', 'value', 'defaultJsValue'
            |       }
            |   }
            |   targetConfigs("dev") {
            |       js {
            |           buildConfigField 'STRING', 'value', 'devJsValue'
            |       }
            |   }
            |}
            |$buildFileMPPConfig
        """.trimMargin()
        )

        val propertyFile = projectDir.newFile("gradle.properties")
        propertyFile.writeText("buildkonfig.flavor=dev")

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace", "-Pbuildkonfig.flavor=nonexistent")
            .build()

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val jvmResult = File(buildDir, "jvmMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jvmResult.readText())
            .contains("defaultValue")

        val jsResult = File(buildDir, "jsMain/com/example/BuildKonfig.kt")
        Truth.assertThat(jsResult.readText())
            .contains("defaultJsValue")

        val iosResult = File(buildDir, "iosMain/com/example/BuildKonfig.kt")
        Truth.assertThat(iosResult.readText())
            .contains("defaultValue")
    }

    @Test
    fun `The generated object uses the given objectName`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |buildkonfig {
            |   packageName = "com.example"
            |   objectName = "AwesomeConfig"
            |
            |   defaultConfigs {
            |       buildConfigField 'STRING', 'value', 'defaultValue'
            |   }
            |}
            |$buildFileMPPConfig
        """.trimMargin()
        )

        val propertyFile = projectDir.newFile("gradle.properties")
        propertyFile.writeText("buildkonfig.flavor=dev")

        val buildDir = File(projectDir.root, "build/buildkonfig")
        buildDir.deleteRecursively()

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()

        val result = runner
            .withArguments("generateBuildKonfig", "--stacktrace")
            .build()

        Truth.assertThat(result.output)
            .contains("BUILD SUCCESSFUL")

        val commonResult = File(buildDir, "commonMain/com/example/AwesomeConfig.kt")
        Truth.assertThat(commonResult.readText()).apply {
            contains("object AwesomeConfig")
        }
    }
}