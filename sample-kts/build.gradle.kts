import com.codingfeline.buildkonfig.compiler.FieldSpec
import java.util.regex.Pattern

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.codingfeline.buildkonfig") version "+"
}
println("test")
data class BuildConfigs(
    val buildType: String = "",
    val flavour: String = "",
    val buildVarient: String = ""
)

var buildConfigs = BuildConfigs()

fun getCurrentBuildConfigs(): BuildConfigs {
    val iOSFlavor = project.findProperty("ios.flavor")
println("iosflavor: $iOSFlavor")
    if (iOSFlavor != null) {
//        return iOSBuildConfigs(iOSFlavor)
    }
    val taskRequestsStr = gradle.startParameter.taskRequests.toString()
    val pattern: Pattern = if (taskRequestsStr.contains("assemble")) {
        Pattern.compile("assemble(\\w+)(Release|Debug)")
    } else {
        Pattern.compile("bundle(\\w+)(Release|Debug)")
    }
    val matcher = pattern.matcher(taskRequestsStr)
    val buildConfigs = if (matcher.find()) {
        val flavour = matcher.group(1).lowercase()
        val buildType = matcher.group(2)
        BuildConfigs(
            flavour = flavour,
            buildType = buildType.lowercase(),
            buildVarient = "${flavour}$buildType"
        )
    } else {
        println("No android product-flavour found!")
        BuildConfigs()
    }
    return buildConfigs
}

kotlin {
    buildConfigs = getCurrentBuildConfigs()
    println("flavor: ${buildConfigs.flavour}")
    jvm()
    js("jsCommon", IR) {
        browser()
        nodejs()
    }
    ios()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    linuxX64()
    mingwX64()

    applyDefaultHierarchyTemplate()

    /**
     * - commonMain
     *   - appMain
     *     - jvmMain
     *     - desktopMain
     *       - macosX64Main
     *       - linuxX64Main
     *       - mingwX64Main
     *   - jsCommonMain
     *   - iosMain
     *     - iosArm64Main
     *     - iosX64Main
     */
    sourceSets {
        val commonMain by getting
        val commonTest by getting

        val appMain by creating {
            dependsOn(commonMain)
        }

        val jvmMain by getting {
            dependsOn(appMain)
        }

        val desktopMain by creating {
            dependsOn(appMain)
        }

        val macosX64Main by getting {
            dependsOn(desktopMain)
        }
        val linuxX64Main by getting {
            dependsOn(desktopMain)
        }
        val mingwX64Main by getting {
            dependsOn(desktopMain)
        }

        val jsCommonMain by getting {
            dependsOn(commonMain)
        }
    }
}

buildkonfig {
    packageName = "com.codingfeline.buildkonfigsample"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "test", "testvalue")
        buildConfigField(FieldSpec.Type.STRING, "target", "common")
        buildConfigField(FieldSpec.Type.STRING, "testKey1", null, nullable = true)
        buildConfigField(FieldSpec.Type.STRING, "testKey2", "testValue2", nullable = false)
        buildConfigField(FieldSpec.Type.STRING, "testKey3", "testValue3", nullable = false, const = true)
    }

    targetConfigs {
        create("jvm") {
            buildConfigField(FieldSpec.Type.STRING, "target", "jvm")
        }
        create("ios") {
            buildConfigField(FieldSpec.Type.STRING, "target", "ios")
        }
        create("desktop") {
            buildConfigField(FieldSpec.Type.STRING, "desktopvalue", "desktop")
        }
        create("jsCommon") {
            buildConfigField(FieldSpec.Type.STRING, "target", "jsCommon")
        }
    }
}

