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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    
    // Use a variable for the version to keep them in sync
    val media3Version = "1.3.1"

    // The core player engine (required)
    implementation("androidx.media3:media3-exoplayer:$media3Version")

    // The UI components, including PlayerView (required for the view)
    implementation("androidx.media3:media3-ui:$media3Version")

    // For background playback and media session integration (highly recommended)
    implementation("androidx.media3:media3-session:$media3Version")
    
    // For HLS streaming support (optional, but common)
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")

    // For DASH streaming support (optional, but common)
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
}
