@file:Suppress("DEPRECATION")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.poojapurohit"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.poojapurohit"
        minSdk = 24
        //noinspection OldTargetApi
        targetSdk = 35
        versionCode = 6
        versionName = "2.1"

        val mapsKey = project.findProperty("MAPS_API_KEY")?.toString() ?: ""
        manifestPlaceholders["MAPS_API_KEY"] = mapsKey

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("C:/Work/Software_Development/Android_Studio_Projects/Release Key/adarsh_release_key.jks")
            storePassword = "ARKtaida#123"
            keyAlias = "key0"
            keyPassword = "ARKtaida#123"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    //noinspection WrongGradleMethod
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        compose = true
        dataBinding = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.compose.animation.core)
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    //Image Loading
    implementation(libs.coil.compose)

    // Compose BOM (manages all Compose versions)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Compose Integration
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Lottie (XML + Compose)
    implementation(libs.lottie)
    implementation(libs.lottie.compose)

    // Firebase
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Credentials & Google Sign-In
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Lifecycle & ViewModel
    implementation(libs.lifecycle.viewmodel.ktx)

    // RecyclerView (keep if used elsewhere)
    implementation(libs.androidx.recyclerview)

    // Flow layout for skills chips
    implementation(libs.accompanist.flowlayout)
    implementation(libs.firebase.messaging)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Compose Testing
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.places)
    implementation(libs.kotlinx.coroutines.play.services)

    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)
}