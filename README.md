BuildKonfig
===

BuildConfig for Kotlin Multiplatform Project


## Usege

BuildKonfig supports Kotlin Multiplatform Project **only**.


### Gradle

```gradle
buildScript {
    dependencies {
        classpath 'com.codingfeline.buildkonfig:buildkonfig-gradle-plugin:latest_version'
    }
}

apply plugin: 'org.jetbrains.kotlin.multiplatform'
apply plugin: 'com.codingfeline.buildkonfig'

kotlin {
    // your target config...
    android()
    iosX64('ios')
}

buildKonfig {
    packageName = 'com.example.app'
    
    // default config is required
    defaultConfigs {
        buildConfigField 'STRING', 'name', 'value'
    }
    
    targetConfigs {
        // this name should be same as target names you specified
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
