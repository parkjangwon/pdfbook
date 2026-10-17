plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.parkjw.apps.pdfbook"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.parkjw.apps.pdfbook"
        minSdk = 35
        targetSdk = 37
        versionCode = (findProperty("versionCode") as String?)?.toIntOrNull()
            ?: System.getenv("PDFBOOK_VERSION_CODE")?.toIntOrNull()
            ?: 1
        versionName = (findProperty("versionName") as String?)
            ?: System.getenv("PDFBOOK_VERSION_NAME")
            ?: "0.0.1"
    }

    signingConfigs {
        val keystorePath = System.getenv("PDFBOOK_KEYSTORE")
        val storePassword = System.getenv("PDFBOOK_STORE_PASSWORD")
        val keyAlias = System.getenv("PDFBOOK_KEY_ALIAS")
        val keyPassword = System.getenv("PDFBOOK_KEY_PASSWORD")
        if (!keystorePath.isNullOrBlank() &&
            !storePassword.isNullOrBlank() &&
            !keyAlias.isNullOrBlank() &&
            !keyPassword.isNullOrBlank()
        ) {
            create("release") {
                storeFile = file(keystorePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
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
            signingConfigs.findByName("release")?.let { signingConfig = it }
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
    testImplementation(libs.junit)
}
