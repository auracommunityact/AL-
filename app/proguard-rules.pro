# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization support
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class *$$serializer { *; }
-keepclassmembers class * {
    *** Companion;
}
-keep class kotlinx.serialization.json.** { *; }

# Keep App Models and Local entities from being stripped/obfuscated
-keep class com.example.data.models.** { *; }
-keep class com.example.data.local.** { *; }

# Gson Rules
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room Rules
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * {
    @androidx.room.ColumnInfo <fields>;
    @androidx.room.PrimaryKey <fields>;
}

# Retrofit & OkHttp Rules
-keepclassmembers class * {
    @retrofit2.http.** <methods>;
}
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Supabase and Ktor
-dontwarn io.ktor.**
-dontwarn io.github.jan.supabase.**
-keep class io.github.jan.supabase.** { *; }

# ML Kit Rules
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-dontwarn com.google.mlkit.**

# Google Mobile Ads (AdMob) Rules
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

