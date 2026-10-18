plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing comes from environment variables (GitHub secrets). Without them the
// committed sideload key is used, so test builds install over each other.
val releaseKeystore = System.getenv("RELEASE_KEYSTORE_PATH")?.takeIf { file(it).exists() }

fun env(name: String, fallback: String) = System.getenv(name)?.takeIf { it.isNotBlank() } ?: fallback

android {
    namespace = "lt.marcomultiservices.skaitaupats"
    compileSdk = 36

    defaultConfig {
        // Lithuanian only: drop translated strings that libraries bring along.
        resourceConfigurations += listOf("lt")
        applicationId = "lt.marcomultiservices.skaitaupats"
        minSdk = 26
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "1.0.${System.getenv("VERSION_CODE") ?: "0"}"
        // AdMob ids come from GitHub secrets; without them Google's test ids are used, which show test ads.
        manifestPlaceholders["admobAppId"] = env("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${env("ADMOB_INTERSTITIAL_ID", "ca-app-pub-3940256099942544/1033173712")}\"")
        buildConfigField("String", "PRIVACY_URL", "\"${env("PRIVACY_URL", "https://example.com/skaitaupats/privatumas.html")}\"")
        // Test builds (signed with the sideload key) open every lesson, since Play Billing only works
        // in apps installed from Google Play. Play uploads are signed with the upload key and stay locked.
        buildConfigField("boolean", "TEST_UNLOCK", (releaseKeystore == null).toString())
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("sideload.keystore")
            storePassword = "strompreisuhr"
            keyAlias = "sideload"
            keyPassword = "strompreisuhr"
        }
        if (releaseKeystore != null) {
            create("upload") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += listOf("json", "mp3")
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(project(":core"))

    val composeBom = platform("androidx.compose:compose-bom:2025.05.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.emoji2:emoji2:1.5.0")
    implementation("androidx.emoji2:emoji2-bundled:1.5.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("com.google.android.gms:play-services-ads:24.3.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")
}
