plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "app.habittracker.android"
    // compileSdk 37 (not 36): lifecycle-runtime-compose 2.11.0 and Compose BOM 2026.09.00 require it. targetSdk stays 36.
    compileSdk = 37

    defaultConfig {
        applicationId = "app.habittracker.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.kotlin.test.junit)
}
