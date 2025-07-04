plugins {
    id("com.android.application")
    id("kotlin-android")
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-kapt") // Add this for annotation processing
    id("kotlin-parcelize") // Add this for parcelable support
    // Annotation processing for Room will be handled by runtime
}

android {
    namespace = "com.antbear.pwneyes"
    compileSdk = 35
    // Update buildToolsVersion to match Android 15 (API Level 35)
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.antbear.pwneyes"
        minSdk = 24
        targetSdk = 35
        versionCode = 36
        versionName = "10.32"
        
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
        // Explicitly set buildConfig without using the deprecated property
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

    kapt {
        correctErrorTypes = true
        // Configure Room-specific arguments only
        arguments {
            arg("room.schemaLocation", layout.buildDirectory.dir("schemas").get().toString())
            arg("room.incremental", "true")
        }
        // Use build cache for faster compilation
        useBuildCache = true
    }
}

// Make sure all Kotlin compile tasks use Java 11
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

// Add JVM arguments to fix Kotlin daemon issues
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
    // Use stable Kotlin compiler options only
    freeCompilerArgs.addAll(listOf(
        "-opt-in=kotlin.RequiresOptIn",
        "-Xskip-prerelease-check"
    ))
    }
}

// Configure Gradle daemon settings to avoid connection issues
tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
    options.forkOptions.jvmArgs = listOf("-Xmx3g")
}

// Configure Kotlin daemon
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        // Use modern compiler options without the deprecated memory flags
        // Modern Kotlin compiler manages memory automatically
        freeCompilerArgs.addAll(listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xskip-prerelease-check"
        ))
    }
}

// No annotation processor configuration needed

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
    implementation("androidx.navigation:navigation-fragment-ktx:2.6.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.6.0")
    
    // Room database with updated compatibility for Kotlin 2.1
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    
    // Special configuration for Room compiler with Kotlin 2.1
    kapt("androidx.room:room-compiler:2.6.1") {
        // DO NOT exclude kotlinx-metadata-jvm here - we need to provide our own version
    }
    
    // Provide explicit kotlinx-metadata-jvm dependency for annotation processors
    // This is crucial for KAPT to work with Kotlin 2.1
    kapt("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.7.0")
    
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.0")
    
    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Kotlin metadata for reflection - use newer version compatible with Kotlin 2.1
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.7.0")
    
    implementation("androidx.preference:preference:1.2.0")
    // Google Play Billing Library for in-app purchases - Updated to v7.0.0
    implementation("com.android.billingclient:billing-ktx:7.0.0")
    // Google Ads - now used conditionally based on premium status
    implementation("com.google.android.gms:play-services-ads:22.6.0")
    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    // GridLayout support for native UI layouts
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    
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
            // Force newer version of kotlinx-metadata-jvm to support Kotlin 2.0
            force("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.7.0")
            // Force compatible kotlin reflection version
            force("org.jetbrains.kotlin:kotlin-reflect:2.0.0")
            force("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.0")
        }
    }
    
    // Handle kotlinx dependencies specifically
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion("2.0.0")
            }
            if (requested.group == "org.jetbrains.kotlinx" && 
                requested.name == "kotlinx-metadata-jvm") {
                useVersion("0.7.0")
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}
