plugins {
    id("com.android.application")
    id("kotlin-android")
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-kapt") // Add this for annotation processing
}

android {
    namespace = "com.antbear.pwneyes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.antbear.pwneyes"
        minSdk = 24
        targetSdk = 34
        versionCode = 24
        versionName = "10.15"
        
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

    // Configure product flavors for correct package name on Google Play
    flavorDimensions += "version"
    productFlavors {
        create("free") {
            dimension = "version"
            applicationIdSuffix = ".free"
            versionNameSuffix = "-free"
        }
        create("paid") {
            dimension = "version"
            applicationIdSuffix = ".paid"
            versionNameSuffix = "-paid"
        }
    }
    
    // Limit build variants (only generate free variants by default)
    // This reduces the number of build variants from 4 to 2
    androidComponents {
        beforeVariants { variantBuilder ->
            // Only generate free variants by default, unless a paid-specific task is explicitly requested
            if (variantBuilder.flavorName == "paid") {
                val paidTaskRequested = gradle.startParameter.taskNames.any { 
                    it.contains("paid", ignoreCase = true) 
                }
                
                if (!paidTaskRequested) {
                    variantBuilder.enable = false
                }
            }
        }
    }
    
    // Configure source sets to ensure proper flavor-specific class selection
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
        }
        getByName("free") {
            java.srcDirs("src/free/java")
        }
        getByName("paid") {
            java.srcDirs("src/paid/java")
        }
    }

    signingConfigs {
        // Use debug signing config for temporary testing
        getByName("debug") {
            // Debug keystore is automatically created by Android build system
        }
        
        // Release signing config with proper keystore
        create("release") {
            // Check if the keystore file exists
            val keystoreFile = file("pwneyes.keystore.jks")
            
            // Only use the keystore if it exists
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = "android" // Default password, can be overridden
                keyAlias = "pwneyes"
                keyPassword = "android" // Default password, can be overridden
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            
            // Use release signing config for Google Play Store uploads
            signingConfig = signingConfigs.getByName("release")
            
            // Disable baseline profiles
            proguardFile("baseline-profiles-rules.pro")
        }
        debug {
            // Disable baseline profiles
            proguardFile("baseline-profiles-rules.pro")
            
            // Use debug signing for debug builds
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    
    buildFeatures {
        viewBinding = true
        // This is marked as deprecated but still needed
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

// Make sure all Kotlin compile tasks use Java 11
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
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
    // Room compiler annotation processor - needed for code generation
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
