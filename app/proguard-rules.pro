# ProGuard / R8 Optimization Rules for AuraHealth

# Keep all models, entities, and database classes
-keep class com.pro.aifitnesstracker.data.** { *; }
-keep class com.pro.aifitnesstracker.models.** { *; }
-keep class com.pro.aifitnesstracker.SharedPrefsManager { *; }
-keep class com.pro.aifitnesstracker.AdConfigManager { *; }

# Room SQLite Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# Google Play Billing, Drive & Firebase
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.android.billingclient.** { *; }

# MLKit & Vision
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Jetpack Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontwarn androidx.compose.**

# Kotlin Coroutines & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
