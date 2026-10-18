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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Orchestrator wipes app data before each test so the DB starts empty.
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
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
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.kotlin.test.junit)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestUtil(libs.androidx.test.orchestrator)
    debugImplementation(libs.compose.ui.test.manifest)
}
