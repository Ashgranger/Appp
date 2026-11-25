# ProGuard / R8 rules for Google Play Store release optimization

# Preserve WebView JavaScript interfaces for password capture & page telemetry
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve stack trace line numbers for Firebase / Play Console crash diagnostics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# OkHttp optional platform warnings
-dontwarn okhttp3.**
-dontwarn okio.**
