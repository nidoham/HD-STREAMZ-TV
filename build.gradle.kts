// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.0.0" apply false
    id("com.android.library") version "8.0.0" apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}