BuildKonfig
===

[ ![Download](https://api.bintray.com/packages/yshrsmz/maven/buildkonfig-gradle-plugin/images/download.svg) ](https://bintray.com/yshrsmz/maven/buildkonfig-gradle-plugin/_latestVersion)

BuildConfig for Kotlin Multiplatform Project.  
It currently supports embedding values from gradle file.

## Motivation

Passing values from Android/iOS or any other platform code should work, but it's a hassle.  
Setting up Android to read values from properties and add those into BuildConfig, and do the equivalent in iOS?  
Rather I'd like to do it once.


## Usege

BuildKonfig supports Kotlin Multiplatform Project **only**.


### Gradle

```gradle
buildScript {
    repositories {
        maven { url 'https://dl.bintray.com/yshrsmz/maven' }
    }
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

buildkonfig {
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

To generate BuildKonfig files, run `generateBuildKonfig` task.  
This task will be automatically run upon execution of kotlin compile tasks.

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
