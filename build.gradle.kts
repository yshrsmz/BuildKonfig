plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.pluginPublish) apply false
    alias(libs.plugins.mavenPublish) apply false
}


val GROUP = project.property("GROUP") as String
val VERSION_NAME = project.property("VERSION_NAME") as String

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    tasks.withType(JavaCompile::class.java) {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    group = GROUP
    version = VERSION_NAME
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.layout.buildDirectory)
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}
