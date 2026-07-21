import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

// ponytail: version derived from git tag; hand-editing is gone. Only tagged CI
// builds ship a real version — untagged local builds fall back to 0.0.0.
fun git(vararg args: String): String = try {
    providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
} catch (_: Exception) { "" }   // no git binary / building from a source zip

val rawTag = git("describe", "--tags", "--abbrev=0", "--match", "v[0-9]*").removePrefix("v")
val isTagBuild = System.getenv("GITHUB_REF")?.startsWith("refs/tags/") == true

// A tag-push CI build that can't resolve its tag must fail, not ship "0.0.0" and
// silently break auto-update (UpdateChecker compares versionName).
if (isTagBuild && rawTag.isEmpty())
    throw GradleException("Tag build but no v-tag resolved; refusing to ship 0.0.0")

val semver = rawTag.ifEmpty { "0.0.0" }
val (verMajor, verMinor, verPatch) = (semver.split(".") + listOf("0", "0", "0"))
    .take(3).map { it.toIntOrNull() ?: 0 }

// Release signing. Credentials come from the environment ONLY (GitHub Secrets in CI) —
// never from a file in this repo. Without them the release build is simply unsigned,
// so local/branch builds still work.
val keystorePath = System.getenv("KEYSTORE_PATH")
val hasKeystore = !keystorePath.isNullOrEmpty() && file(keystorePath).exists()

// An unsigned APK cannot be installed, and a release signed with a *different* key
// cannot update an existing install. Publishing either from a tag build would ship a
// broken update, so fail loudly instead — same principle as the version guard above.
if (isTagBuild && !hasKeystore)
    throw GradleException(
        "Tag build without a signing keystore; refusing to publish an uninstallable " +
            "unsigned APK. Set the KEYSTORE_BASE64 / KEYSTORE_PASSWORD / KEY_ALIAS / " +
            "KEY_PASSWORD repository secrets."
    )

android {
    namespace = "com.antbear.pwneyes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.antbear.pwneyes"
        minSdk = 24
        targetSdk = 34
        versionCode = verMajor * 10000 + verMinor * 100 + verPatch   // v1.1.1 -> 10101
        versionName = semver
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasKeystore) {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Signed when the keystore env is present; otherwise unsigned (and a tag
            // build would already have failed the guard above).
            signingConfig = if (hasKeystore) signingConfigs.getByName("release") else null
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true   // AGP 8+ disables BuildConfig by default; required for BuildConfig.VERSION_NAME
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.swiperefreshlayout)
    kapt(libs.androidx.room.compiler)
}
