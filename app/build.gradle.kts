import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.wallpaintai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.wallpaintai"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val keystoreFile = project.rootProject.file("local.properties")
        val properties = Properties()
        properties.load(keystoreFile.inputStream())

        //return empty key in case something goes wrong
        val apiKey = properties.getProperty("API_KEY") ?: ""

        buildConfigField(
            type = "String",
            name = "API_KEY",
            value = apiKey
        )

        multiDexEnabled=true

    }
    buildFeatures{
        buildConfig = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.benchmark.macro)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.google.ar.sceneform.ux:sceneform-ux:1.17.1")
    implementation("com.google.ar.sceneform:rendering:1.17.1")
    implementation("com.google.ar:core:1.44.0")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("androidx.concurrent:concurrent-futures:1.2.0")
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.6.0")

//    implementation("com.google.code.gson:gson:2.10.1")
//    implementation("com.squareup.retrofit2:retrofit:2.9.0")
//    implementation("com.squareup.retrofit2:converter-gson:2.5.0")
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("com.beust:klaxon:5.5")
    implementation("com.android.support:multidex:1.0.3")

}