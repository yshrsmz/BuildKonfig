BuildKonfig
===

BuildConfig for Kotlin Multiplatform Project


## Usege

```gradle
plugins {
    id 'kotlin-multiplatform'
    id 'com.codingfeline.buildkonfig'
}

kotlin {
    android()
    iosX64('ios)
}

buildKonfig {
    defaultConfigs {
        buildConfigField 'STRING', 'name', 'value'
    }
    
    targetConfigs {
        // this name should be same as target name you specified
        android {
            buildConfigField 'STRING', 'name2', 'value2'
        }
        
        ios {
            buildConfigField 'STRING', 'name', 'valueForNative'
        }
    }
}
```

Above configuration will generate following codes.

```kotlin
// commonMain
expect object BuildKonfig {
    val name: String
}
```

```kotlin
// androidMain
actual object BuildKonfig {
    actual val name: String = "value"
    val name2: String = "value2"
}
```

```kotlin
// iosMain
actual object BuildKonfig {
    actual val name: String = "valueForNative"
}
```


## Supported Types

- String
- Int
- Long
- Float
- Boolean
