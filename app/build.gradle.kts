plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val siteHost = (project.findProperty("SITE_HOST") as String)

android {
    namespace = "com.nagargo.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.nagargo.app"
        minSdk = 24
        targetSdk = 34
        versionCode = (System.getenv("RUN_NUMBER")?.toIntOrNull() ?: 0) + 100
        versionName = "1.${System.getenv("RUN_NUMBER") ?: "0"}"
        manifestPlaceholders["siteHost"] = siteHost
        buildConfigField("String", "SITE_URL", "\"https://$siteHost/\"")
        buildConfigField("String", "SITE_HOST", "\"$siteHost\"")
    }
    buildFeatures { buildConfig = true }
    val ksPath = System.getenv("KEYSTORE_PATH")
    signingConfigs {
        if (ksPath != null) create("rel") {
            storeFile = file(ksPath)
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (ksPath != null) signingConfig = signingConfigs.getByName("rel")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.browser:browser:1.8.0")
}
