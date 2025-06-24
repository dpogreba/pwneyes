plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("androidx.navigation.safeargs.kotlin") version "2.6.0" apply false
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}
