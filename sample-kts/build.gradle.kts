import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.codingfeline.buildkonfig") version "+"
}

kotlin {
    jvm()
    js("jsCommon", IR) {
        browser()
        nodejs()
    }
    iosArm64()
    iosX64()
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
     *     - iosSimulatorArm64Main
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
