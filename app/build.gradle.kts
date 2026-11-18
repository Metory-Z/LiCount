plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.licount"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.licount"
        minSdk = 26
        targetSdk = 34
        // 随 CI 运行号递增，保证每次出包都比上一版新，覆盖安装不会被「版本过旧」拒绝
        versionCode = 1000 + (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0)
        versionName = "1.0.0"
    }

    // 固定签名：CI 由 GitHub Secrets 注入，本地可指向项目根目录的 licount-release.jks。
    // 所有可供安装的 APK 必须用同一把密钥，否则 Android 会因签名不一致拒绝覆盖安装。
    val signingPath = System.getenv("SIGNING_KEY_PATH")
    signingConfigs {
        create("fixed") {
            storeFile = signingPath?.let { rootProject.file(it) }
            storePassword = System.getenv("KEY_STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            // 不再加 applicationIdSuffix：debug 与 release 统一为 com.licount，
            // 避免「包名不一致」导致必须卸载旧版才能安装。
            versionNameSuffix = "-debug"
            if (signingPath != null) signingConfig = signingConfigs.getByName("fixed")
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 发布包必须用固定签名；缺少密钥时构建失败，绝不静默换成随机签名
            signingConfig = signingConfigs.getByName("fixed")
        }
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room 持久层（SQLite）
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // DataStore 偏好设置
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
