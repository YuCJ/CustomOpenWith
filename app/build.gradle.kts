plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.yucj.customopenwith"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yucj.customopenwith"
        minSdk = 33
        targetSdk = 34
        // CI 注入正式版本（-PappVersionCode/-PappVersionName）；本機 build 用 -dev 後綴，
        // 更新檢查把 -dev 視為比任何正式版舊。
        versionCode = providers.gradleProperty("appVersionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("appVersionName").orNull ?: "1.0.0-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // CI 從 repo secrets 還原 keystore 後以環境變數提供；
        // 本機沒設這些變數時退回 debug key。更新安裝要求簽章一致，
        // 正式發佈一律走 CI 的固定 key。
        val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
        val keystorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
        if (keystorePath != null && keystorePassword != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = "release"
                keyPassword = keystorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.recyclerview)
}