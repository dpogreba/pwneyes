plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.antbear.pwneyes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.antbear.pwneyes"
        minSdk = 24
        targetSdk = 34
        versionCode = 12
        versionName = "10.3"
        
        // Explicitly disable baseline profiles to fix installation issues
        ndk {
            abiFilters.clear()
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    // Disable lint checks for now
    lint {
        abortOnError = false
    }

    // Removed product flavors (free/paid) as we're using in-app purchases instead

    signingConfigs {
        create("release") {
            // Check if the keystore file exists
            val keystoreFile = file("pwneyes.keystore.jks")
            
            // Only use the keystore if it exists and passwords are provided
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                
                // Use empty string as fallback for missing properties
                storePassword = project.findProperty("KEYSTORE_PASSWORD")?.toString() ?: ""
                keyAlias = "pwneyes"
                keyPassword = project.findProperty("KEY_PASSWORD")?.toString() ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            
            // Temporarily disable signing to avoid keystore password issues
            signingConfig = null
            
            // Disable baseline profiles
            proguardFile("baseline-profiles-rules.pro")
        }
        debug {
            // Disable baseline profiles
            proguardFile("baseline-profiles-rules.pro")
            
            // No signing for debug builds
            signingConfig = null
        }
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

// Configure kapt to use the same Java version
kapt {
    javacOptions {
        option("-source", "11")
        option("-target", "11")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("com.google.code.gson:gson:2.10.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.preference:preference:1.2.0")
    // Google Play Billing Library for in-app purchases
    implementation("com.android.billingclient:billing-ktx:6.0.1")
    // Google Ads - now used conditionally based on premium status
    implementation("com.google.android.gms:play-services-ads:22.6.0")
    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    
    // Explicitly exclude the profileinstaller
    configurations.all {
        exclude(group = "androidx.profileinstaller", module = "profileinstaller")
    }
}
