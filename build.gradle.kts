// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    val kotlinversion = ("1.8.22")
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath (libs.gradle)
        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinversion")
        classpath ("org.jetbrains.kotlin:kotlin-serialization:$kotlinversion")

        // NOTE: Do not place your application dependencies here, they belong
        // to the individual module build.gradle files
    }
}

plugins {
    id ("com.google.devtools.ksp") version ("1.9.0-1.0.12") apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}