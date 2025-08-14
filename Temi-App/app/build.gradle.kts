import com.android.build.api.dsl.AaptOptions

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.mqtt"
    compileSdk = 34


    defaultConfig {
        applicationId = "com.example.mqtt"
        minSdk = 23
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    aaptOptions {
        noCompress("tflite")
    }

    kotlinOptions {
        jvmTarget = "17"
    }


}



dependencies {
    implementation("org.opencv:opencv:4.11.0")

    implementation("org.tensorflow:tensorflow-lite:2.4.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.4.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.1.0")
    implementation ("org.tensorflow:tensorflow-android:+")

    val camerax_version = "1.1.0-alpha09"

    // CameraX core library
    implementation ("androidx.camera:camera-core:$camerax_version")
    // CameraX Camera2 extension library
    implementation ("androidx.camera:camera-camera2:$camerax_version")
    // CameraX Lifecycle library
    implementation ("androidx.camera:camera-lifecycle:$camerax_version")
    // CameraX View class
    implementation ("androidx.camera:camera-view:1.0.0-alpha31")


    implementation ("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("com.robotemi:sdk:1.131.4")

    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation ("io.coil-kt:coil:1.4.0")
    implementation ("com.squareup.okhttp3:okhttp:4.9.1")
    // added MLKit dependencies for face detector
    implementation ("com.google.mlkit:face-detection:16.0.0")


    implementation ("org.tensorflow:tensorflow-android:+")

    implementation ("com.airbnb.android:lottie:6.3.0")


}