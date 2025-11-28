plugins {
    alias(libs.plugins.androidTest) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidx.navigation.safeargs) apply false
    alias(libs.plugins.kotlin.serialization) apply false
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