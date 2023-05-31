import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    @Suppress("DSL_SCOPE_VIOLATION") // See also, https://github.com/gradle/gradle/issues/22797#issuecomment-1517046458
    run {
        alias(libs.plugins.android.library)
        alias(libs.plugins.kotlin.multiplatform)
    }
    id("com.codingfeline.buildkonfig")
}

android {
    namespace = "com.example.namespace"
    compileSdk = 28

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {
    android {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    jvm()
    js(IR) {
        browser()
        // See, https://kotlinlang.org/docs/multiplatform-set-up-targets.html#distinguish-several-targets-for-one-platform
        attributes.attribute(Attribute.of("com.example.target", String::class.java), "browser")
    }
    js("node", IR) {
        nodejs()
    }
    ios()
    macosX64()
    linuxX64()
    mingwX64()

    /**
     * - commonMain
     *   - appMain
     *     - androidMain
     *     - jvmMain
     *     - desktopMain
     *       - macosX64Main
     *       - linuxX64Main
     *       - mingwX64Main
     *   - jsCommonMain
     *     - jsMain
     *     - nodeMain
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

        val androidMain by getting {
            dependsOn(appMain)
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

        val jsCommonMain by creating {
            dependsOn(commonMain)
        }
        val jsMain by getting {
            dependsOn(jsCommonMain)
        }
        val nodeMain by getting {
            dependsOn(jsCommonMain)
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
        create("android") {
            buildConfigField(FieldSpec.Type.STRING, "target", "android")
        }
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
