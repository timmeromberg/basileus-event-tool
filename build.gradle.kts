plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.compose") version "1.7.1"
}

group = "com.basileus"
version = "0.1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)

    // Koin for DI
    implementation("io.insert-koin:koin-core:3.5.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

compose.desktop {
    application {
        mainClass = "com.basileus.eventtool.EventToolMainKt"
        nativeDistributions {
            targetFormats = setOf(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg)
            packageName = "Basileus Event Tool"
            packageVersion = "1.0.0"
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(18))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "18"
    kotlinOptions.freeCompilerArgs += listOf(
        "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
        "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi"
    )
}
