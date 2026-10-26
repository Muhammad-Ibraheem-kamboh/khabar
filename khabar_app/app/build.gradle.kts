import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val msClientId = localProperties.getProperty("MS_CLIENT_ID", "") ?: ""
val msSignatureHash = localProperties.getProperty("MS_SIGNATURE_HASH", "") ?: ""
val gmailEnabled = (localProperties.getProperty("GMAIL_ENABLED", "false") ?: "false").toBoolean()
val gmailClientId = localProperties.getProperty("GMAIL_CLIENT_ID", "") ?: ""
val aiBaseUrl = localProperties.getProperty("AI_BASE_URL", "") ?: ""
val aiModel = localProperties.getProperty("AI_MODEL", "") ?: ""
val aiApiKey = localProperties.getProperty("AI_API_KEY", "") ?: ""

android {
    namespace = "com.khabar.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.khabar.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "MS_CLIENT_ID", "\"$msClientId\"")
        buildConfigField("String", "MS_SIGNATURE_HASH", "\"$msSignatureHash\"")
        buildConfigField("boolean", "GMAIL_ENABLED", gmailEnabled.toString())
        buildConfigField("String", "GMAIL_CLIENT_ID", "\"$gmailClientId\"")
        buildConfigField("String", "AI_BASE_URL", "\"$aiBaseUrl\"")
        buildConfigField("String", "AI_MODEL", "\"$aiModel\"")
        buildConfigField("String", "AI_API_KEY", "\"$aiApiKey\"")

        manifestPlaceholders["msSignatureHash"] = if (msSignatureHash.isEmpty()) "placeholder_hash" else msSignatureHash
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.configureEach {
    if (name.contains("lint", ignoreCase = true)) {
        enabled = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui.text.google.fonts)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Image loading
    implementation(libs.coil.compose)

    // Networking & Serialization
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // ML Kit On-Device Text Recognition
    implementation(libs.mlkit.text.recognition)

    // MSAL & Play Services Auth
    implementation(libs.msal)
    implementation(libs.play.services.auth)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
