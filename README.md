BuildKonfig
===

BuildConfig for Kotlin Multiplatform Project


## Usege

```gradle
apply plugin: 'com.codingfeline.buildkonfig'

buildKonfig {
    defaultConfigs {
        buildConfigField 'String', 'name', 'value'
    }
    
    targetConfigs {
        android {
            buildConfigField 'String', 'name2', 'value2'
        }
        
        'native' {
            buildConfigField 'String', 'name', 'valueForNative'
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
// nativeMain
actual object BuildKonfig {
    actual val name: String = "valueForNative"
}
```
