import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

val releaseStorePath = keystoreProperties.getProperty("storeFile")
    ?: System.getenv("NOVA_KEYSTORE_PATH")
val releaseStorePassword = keystoreProperties.getProperty("storePassword")
    ?: System.getenv("NOVA_KEYSTORE_PASSWORD")
val releaseKeyAlias = keystoreProperties.getProperty("keyAlias")
    ?: System.getenv("NOVA_KEY_ALIAS")
val releaseKeyPassword = keystoreProperties.getProperty("keyPassword")
    ?: System.getenv("NOVA_KEY_PASSWORD")

val hasReleaseSigning = !releaseStorePath.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank() &&
    rootProject.file(releaseStorePath).exists()

val resolvedVersionCode = (findProperty("NOVA_VERSION_CODE") as String?)?.toIntOrNull()
    ?: System.getenv("NOVA_VERSION_CODE")?.toIntOrNull()
    ?: 21

val resolvedVersionName = (findProperty("NOVA_VERSION_NAME") as String?)?.takeIf { it.isNotBlank() }
    ?: System.getenv("NOVA_VERSION_NAME")?.takeIf { it.isNotBlank() }
    ?: "1.2.0"

val resolvedUpdateConfigUrl = (findProperty("NOVA_UPDATE_CONFIG_URL") as String?)?.takeIf { it.isNotBlank() }
    ?: System.getenv("NOVA_UPDATE_CONFIG_URL")?.takeIf { it.isNotBlank() }
    ?: "https://nova-website-1tg.pages.dev/update.json"

base {
    archivesName.set("NOVA-MUSIC-v$resolvedVersionName")
}

android {
    namespace = "com.novamusic.player"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.novamusic.player"
        minSdk = 26
        targetSdk = 35
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName

        buildConfigField("String", "UPDATE_CONFIG_URL", "\"$resolvedUpdateConfigUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = false
        disable += setOf("MissingTranslation", "ExtraTranslation")
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.google.material)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
}
