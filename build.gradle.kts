// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.kotlin.serialization)
        classpath(libs.androidx.navigation.safeargs)

        // NOTE: Do not place your application dependencies here, they belong
        // in the individual module build.gradle.kts files
    }
}

allprojects {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }
}

plugins {
    alias(libs.plugins.androidTest) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.androidApplication) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

// this builds the list of languages to use for Android versions below 13
tasks.register("buildLanguages") {
    val projectDirectory = layout.projectDirectory

    // reference: https://docs.gradle.org/current/userguide/working_with_files.html
    val resPath = projectDirectory.file("app/src/main/res")
    val locales = resPath.asFile.listFiles()
        .filter {
            it.nameWithoutExtension.startsWith("values-") && File(
                it,
                "strings.xml"
            ).exists()
        }
        .map {
            it.nameWithoutExtension.removePrefix("values-")
        } + "en" // en is the default locale, its values file has no -en suffix

    val localesConfig =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n" +
                "<string-array name=\"languageCodes\">\n" +
                locales.joinToString("\n") { "  <item>$it</item>" } + "\n" +
                "</string-array>\n" +
                "</resources>"

    val outputFile = projectDirectory.file("app/src/main/res/values/languages.xml").asFile
    if (!outputFile.exists()) outputFile.createNewFile()
    outputFile.bufferedWriter().use {
        it.write(localesConfig)
    }
}