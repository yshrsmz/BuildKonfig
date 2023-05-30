import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    @Suppress("DSL_SCOPE_VIOLATION") // See also, https://github.com/gradle/gradle/issues/22797#issuecomment-1517046458
    run {
        alias(libs.plugins.android.library)
        alias(libs.plugins.kotlin.android)
    }
    id("com.codingfeline.buildkonfig")
}

android {
    namespace = "com.example.namespace"
    compileSdk = 28
}

buildkonfig {
    packageName = "com.codingfeline.buildkonfigsample"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "test", "testvalue")
        buildConfigField(FieldSpec.Type.STRING, "target", "main")
        buildConfigField(FieldSpec.Type.STRING, "testKey1", null, nullable = true)
        buildConfigField(FieldSpec.Type.STRING, "testKey2", "testValue2", nullable = false)
        buildConfigField(FieldSpec.Type.STRING, "testKey3", "testValue3", nullable = false, const = true)
    }
}
