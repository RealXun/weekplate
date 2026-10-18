plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.cleanroom.weekplate"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cleanroom.weekplate"
        minSdk = 32
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0-cleanroom"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

dependencies {
    implementation(project(":core"))

    val composeVersion = "1.8.3"
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")
    implementation("androidx.work:work-runtime:2.12.0")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.compose.material3:material3:1.3.2")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
}
