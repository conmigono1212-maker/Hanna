plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.serialization")
 id("org.jetbrains.kotlin.plugin.compose")
}

android {
 namespace="com.cyberassistant"
 compileSdk=36
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
 buildFeatures { compose = true }
 defaultConfig {
  applicationId="com.cyberassistant"
  minSdk=26
  targetSdk=36
  versionCode=3
  versionName="1.0.0"
 }
}

dependencies {
 implementation("androidx.core:core-ktx:1.17.0")
 implementation("androidx.activity:activity-compose:1.11.0")
 implementation("androidx.compose.ui:ui:1.9.2")
 implementation("androidx.compose.material3:material3:1.3.2")
 implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
 implementation("androidx.work:work-runtime-ktx:2.10.5")
 implementation("androidx.room:room-runtime:2.8.2")
 implementation("androidx.room:room-ktx:2.8.2")
 implementation("androidx.security:security-crypto:1.1.0-alpha06")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

kotlin {
 compilerOptions {
  jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
 }
}
