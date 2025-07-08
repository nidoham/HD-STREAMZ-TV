plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics") // Optional but useful
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
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    lint {
        abortOnError = false // Prevent Lint from crashing CI
        checkReleaseBuilds = false
        textReport = true
        textOutput("stdout")
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // WorkManager
    val androidxWorkVersion = "2.8.1"
    implementation("androidx.work:work-runtime-ktx:$androidxWorkVersion")
    implementation("androidx.work:work-rxjava3:$androidxWorkVersion")

    implementation("androidx.webkit:webkit:1.9.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-crashlytics")

    implementation("androidx.preference:preference:1.2.1")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.12.0")

    // RxJava3
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    // NewPipe Extractor
    val newPipe = "v0.24.6"
    implementation("com.github.TeamNewPipe.NewPipeExtractor:NewPipeExtractor:$newPipe") {
        exclude(group = "org.mozilla", module = "rhino")
    }
    implementation("org.mozilla:rhino:1.7.13") // Safe Rhino version

    implementation("com.github.TeamNewPipe:nanojson:1d9e1aea9049fc9f85e68b43ba39fe7be1c1f751")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Media3 (ExoPlayer)
    val media3Version = "1.3.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")

    // Markwon (Markdown rendering)
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:linkify:4.6.2")

    // PrettyTime
    implementation("org.ocpsoft.prettytime:prettytime:5.0.7.Final")
}

// ✅ Force safe Rhino version
configurations.all {
    resolutionStrategy {
        force("org.mozilla:rhino:1.7.13")
    }
}
