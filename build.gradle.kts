// AGP 9 compiles Kotlin itself and refuses the `org.jetbrains.kotlin.android` plugin.
// It takes the compiler from the kotlin-gradle-plugin jar on this classpath, so this
// is where the Kotlin version is pinned. Code On The Go injects the on-device Maven
// repo here so it resolves offline.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.joom.paranoid:paranoid-gradle-plugin:0.3.14")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    id("com.google.gms.google-services") version "4.6.0" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
