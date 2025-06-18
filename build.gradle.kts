// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.androidx.navigation.safe.args.gradle.plugin)
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

// Force Kotlin version to be 2.1.0 for all projects
allprojects {
    // Apply a resolution strategy to all configurations
    configurations.all {
        resolutionStrategy.eachDependency { details ->
            // Force all Kotlin dependencies to use version 2.1.0
            if (details.requested.group == "org.jetbrains.kotlin") {
                details.useVersion("2.1.0")
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}
