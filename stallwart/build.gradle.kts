import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

val versionProps = Properties().apply {
    file("../version.properties").inputStream().use { load(it) }
}
val libraryVersion: String = versionProps.getProperty("VERSION_NAME")

android {
    namespace = "com.atritripathi.stallwart"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-Xexplicit-api=strict"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.lifecycle.process)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Maven Central publishing via vanniktech plugin
mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("com.atritripathi", "stallwart", libraryVersion)

    pom {
        name.set("Stallwart")
        description.set("A reliable ANR detection library for Android that captures the exact code causing UI freezes")
        url.set("https://github.com/AtriTripathi/Stallwart")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("atritripathi")
                name.set("Atri Tripathi")
                url.set("https://github.com/AtriTripathi")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/AtriTripathi/Stallwart.git")
            developerConnection.set("scm:git:ssh://github.com:AtriTripathi/Stallwart.git")
            url.set("https://github.com/AtriTripathi/Stallwart")
        }
    }
}
