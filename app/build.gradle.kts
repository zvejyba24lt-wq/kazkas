plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Release signing comes from environment variables (GitHub secrets). Without them the
// committed sideload key is used, so test builds install over each other.
// The Google Play bundle (PLAY_BUILD=1) is signed with the upload key; the test APK always with
// the committed sideload key, so it installs over earlier test builds.
val playBuild = System.getenv("PLAY_BUILD") == "1"
val releaseKeystore = System.getenv("RELEASE_KEYSTORE_PATH")?.takeIf { playBuild && it.isNotBlank() && file(it).exists() }

// A Google Play build must not ship test ads or a placeholder privacy link.
if (playBuild) {
    listOf("PRIVACY_URL", "ADMOB_APP_ID", "ADMOB_INTERSTITIAL_ID", "ADMOB_REWARDED_ID").filter { System.getenv(it).isNullOrBlank() }.let { missing ->
        require(missing.isEmpty()) { "Google Play build needs these secrets: $missing" }
    }
}

fun env(name: String, fallback: String) = System.getenv(name)?.takeIf { it.isNotBlank() } ?: fallback

android {
    namespace = "lt.marcomultiservices.kimba"
    compileSdk = 36

    defaultConfig {
        applicationId = "lt.marcomultiservices.kimba"
        minSdk = 26
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "1.0.${System.getenv("VERSION_CODE") ?: "0"}"
        // AdMob ids come from GitHub secrets; without them Google's test ids are used, which show test ads.
        manifestPlaceholders["admobAppId"] = env("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${env("ADMOB_INTERSTITIAL_ID", "ca-app-pub-3940256099942544/1033173712")}\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"${env("ADMOB_REWARDED_ID", "ca-app-pub-3940256099942544/5224354917")}\"")
        // Screenshot and test hooks (launch extras) only in builds not signed for Google Play.
        buildConfigField("boolean", "DEMO_HOOKS", (!playBuild).toString())
        buildConfigField("String", "PRIVACY_URL", "\"${env("PRIVACY_URL", "https://zvejyba24.lt/kimba/privatumas.html")}\"")
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
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
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
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    implementation("com.google.android.gms:play-services-ads:24.3.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")
    implementation("com.android.billingclient:billing-ktx:8.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
