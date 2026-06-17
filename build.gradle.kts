import org.gradle.api.tasks.testing.Test
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import java.io.File

plugins {
    kotlin("jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

val configuredAndroidStudioPath = providers.gradleProperty("androidStudioLocalPath").orNull
    ?: System.getenv("ANDROID_STUDIO_PATH")
val defaultAndroidStudioPath = "/Applications/Android Studio.app/Contents"
val localAndroidStudio = sequenceOf(configuredAndroidStudioPath, defaultAndroidStudioPath)
    .filterNotNull()
    .map(::File)
    .firstOrNull(File::exists)

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("com.github.vlsi.mxgraph:jgraphx:4.2.2")

    intellijPlatform {
        if (localAndroidStudio != null) {
            local(localAndroidStudio.absolutePath)
        } else {
            androidStudio("2025.3.4.6") {
                useInstaller = false
            }
        }
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    withType<Test>().configureEach {
        useJUnit()
    }
}
