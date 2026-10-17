plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val hasReleaseSigning =
    listOf(
        System.getenv("PDFBOOK_KEYSTORE"),
        System.getenv("PDFBOOK_STORE_PASSWORD"),
        System.getenv("PDFBOOK_KEY_ALIAS"),
        System.getenv("PDFBOOK_KEY_PASSWORD"),
    ).all { !it.isNullOrBlank() }

android {
    namespace = "org.parkjw.apps.pdfbook"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.parkjw.apps.pdfbook"
        minSdk = 35
        targetSdk = 37
        versionCode = findProperty("versionCode")?.toString()?.toIntOrNull()
            ?: System.getenv("PDFBOOK_VERSION_CODE")?.toIntOrNull()
            ?: 1
        versionName = findProperty("versionName")?.toString()
            ?: System.getenv("PDFBOOK_VERSION_NAME")
            ?: "0.0.1"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(System.getenv("PDFBOOK_KEYSTORE")!!)
                storePassword = System.getenv("PDFBOOK_STORE_PASSWORD")
                keyAlias = System.getenv("PDFBOOK_KEY_ALIAS")
                keyPassword = System.getenv("PDFBOOK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

gradle.taskGraph.whenReady {
    val needsReleaseSigning =
        allTasks.any { task ->
            Regex("^(assemble|bundle|package|sign).*Release").containsMatchIn(task.name)
        }
    if (needsReleaseSigning && !hasReleaseSigning) {
        throw GradleException(
            "Release signing requires PDFBOOK_KEYSTORE, PDFBOOK_STORE_PASSWORD, PDFBOOK_KEY_ALIAS, and PDFBOOK_KEY_PASSWORD",
        )
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.window)
    implementation(libs.datastore.preferences)
    testImplementation(libs.junit)
}
