# -----------------------------
# Firebase
# -----------------------------
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Firebase Crashlytics
-keep class com.google.firebase.crashlytics.** { *; }

# -----------------------------
# Glide
# -----------------------------
-keep class com.bumptech.glide.** { *; }
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.AppGlideModule
-dontwarn com.bumptech.glide.**

# -----------------------------
# OkHttp
# -----------------------------
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# -----------------------------
# RxJava3
# -----------------------------
-dontwarn io.reactivex.rxjava3.**
-keep class io.reactivex.rxjava3.** { *; }

# -----------------------------
# AndroidX + Media3 / ExoPlayer
# -----------------------------
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# -----------------------------
# WebView JS Interface (Optional, if used)
# -----------------------------
# Uncomment and modify if you're using JS bridges
# -keepclassmembers class com.your.package.YourWebInterface {
#     public *;
# }

# -----------------------------
# PrettyTime
# -----------------------------
-keep class org.ocpsoft.prettytime.** { *; }
-dontwarn org.ocpsoft.prettytime.**

# -----------------------------
# Markwon (Markdown)
# -----------------------------
-dontwarn io.noties.markwon.**
-keep class io.noties.markwon.** { *; }

# -----------------------------
# NewPipe Extractor + Rhino
# -----------------------------
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**

# -----------------------------
# WorkManager
# -----------------------------
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# -----------------------------
# ViewBinding / Activities / Fragments
# -----------------------------
-keepclassmembers class * {
    public <init>(android.content.Context);
}
-keep class * extends android.app.Activity
-keep class * extends androidx.fragment.app.Fragment

# -----------------------------
# Preserve Line Numbers (for crash traces)
# -----------------------------
-keepattributes SourceFile,LineNumberTable

# -----------------------------
# Obfuscation Report
# -----------------------------
-printmapping build/outputs/mapping.txt
