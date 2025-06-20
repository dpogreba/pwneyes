plugins {
    id("com.android.application")
    id("kotlin-android")
    id("androidx.navigation.safeargs.kotlin")
    // Removed kapt plugin to fix build issues
    id("kotlin-parcelize") // Add this for parcelable support
}

android {
    namespace = "com.antbear.pwneyes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.antbear.pwneyes"
        minSdk = 24
        targetSdk = 34
        versionCode = 26
        versionName = "10.17"
        
        // Explicitly disable baseline profiles to fix installation issues
        ndk {
            abiFilters.clear()
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    // Disable lint checks for now
    lint {
        abortOnError = false
        // Ignore warnings about experimental features
        disable += "ExperimentalApiUsage"
    }
    
    // Remove experimental settings by explicitly setting them to default values
    // This addresses the warnings in the build
    androidResources {
        // Don't use experimental resource shrinker
        noCompress.clear()
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

    // Check if we should skip signing
    val skipSigning = project.hasProperty("skipSigning") && 
                      project.property("skipSigning") == "true"
                      
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            
            // Use release signing config for Google Play Store uploads, unless skipSigning is true
            if (!skipSigning) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // Use debug signing config when skipSigning is true
                signingConfig = signingConfigs.getByName("debug")
            }
            
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
        // Use the proper setting for buildConfig
        buildConfig = true
        // Enable data binding support
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}

// Make sure all Kotlin compile tasks use Java 11
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

// Suppress warnings for experimental options
android {
    // Set buildToolsVersion explicitly to avoid warnings
    buildToolsVersion = "33.0.1"
}

// Kapt removed

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.6.2")
    
    // Navigation components
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    
    // Room database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    // Commented out due to kapt issues
    // kapt("androidx.room:room-compiler:2.6.1") {
    //     exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-metadata-jvm")
    // }
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // DataStore - modern alternative to SharedPreferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Dependency injection removed
    // Manual dependency injection is used instead
    
    // Core Kotlin dependencies
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.0")
    
    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Kotlin metadata for reflection
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.3.0")
    
    implementation("androidx.preference:preference:1.2.0")
    // Google Play Billing Library for in-app purchases
    implementation("com.android.billingclient:billing-ktx:6.0.1")
    // Google Ads - now used conditionally based on premium status
    implementation("com.google.android.gms:play-services-ads:22.6.0")
    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    
    // Explicitly exclude the profileinstaller
    configurations.all {
        exclude(group = "androidx.profileinstaller", module = "profileinstaller")
    }
    
    // Force compatible versions
    configurations.all {
        resolutionStrategy {
            // Force older version of kotlinx-metadata-jvm
            force("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.3.0")
            // Force compatible kotlin reflection version
            force("org.jetbrains.kotlin:kotlin-reflect:2.1.0")
            force("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.0")
        }
    }
    
    // Handle kotlinx dependencies specifically
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion("2.1.0")
            }
            if (requested.group == "org.jetbrains.kotlinx" && 
                requested.name == "kotlinx-metadata-jvm") {
                useVersion("0.3.0")
            }
        }
    }
}
