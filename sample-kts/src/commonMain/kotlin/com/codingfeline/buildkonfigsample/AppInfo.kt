package com.codingfeline.buildkonfigsample

object AppInfo {
    val summary: String = buildString {
        append("test=").append(BuildKonfig.test)
        append(", target=").append(BuildKonfig.target)
        append(", testKey1=").append(BuildKonfig.testKey1)
        append(", testKey2=").append(BuildKonfig.testKey2)
        append(", testKey3=").append(BuildKonfig.testKey3)
    }
}
