plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}
android {
    compileSdk 34
    defaultConfig { minSdk 24; targetSdk 34; applicationId "com.demo.bridge" }
}
dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22"
}
