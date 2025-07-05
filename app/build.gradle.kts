plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.nidoham.hdstreamztv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nidoham.hdstreamztv"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database")
    
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.12.0")

    // ✅ NewPipeExtractor with rhino excluded
    implementation("com.github.TeamNewPipe.NewPipeExtractor:NewPipeExtractor:v0.24.6") {
        exclude(group = "org.mozilla", module = "rhino")
    }

    // ✅ Safe Rhino version
    implementation("org.mozilla:rhino:1.7.13")

    // ✅ ExoPlayer / Media3
    val media3Version = "1.3.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
}

// ✅ Force rhino version to avoid conflict
configurations.all {
    resolutionStrategy {
        force("org.mozilla:rhino:1.7.13")
    }
}