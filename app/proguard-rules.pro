# =============================================================================
# ProGuard Rules for FireTV Android TV App
# =============================================================================
# Kept classes: Retrofit, Room, kotlinx.serialization, Coil
# Tested with release builds on Fire TV devices
# =============================================================================

# --- Retrofit ---
-keepattributes Signature, Exceptions, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# --- OkHttp / OKIO ---
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# --- Room Database ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.Dao {
    public *;
}
-dontwarn android.database.**

# --- kotlinx.serialization ---
-keepattributes *Serialization*
-keepclassmembers class ** {
    @kotlinx.serialization.* <fields>;
    @kotlinx.serialization.* <methods>;
}
-keepclassmembers class kotlinx.serialization.internal.** {
    <init>(...);
}

# --- Media3 / ExoPlayer ---
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }

# --- Coil (Image Loading) ---
-dontwarn coil.**
-keep class coil.** { *; }
-keep interface coil.** { *; }

# --- DataStore ---
-dontwarn androidx.datastore.**

# --- Coroutines ---
-keepclassmembers class kotlinx.coroutines.** { *; }

# --- AndroidX Core / Compose ---
-keepattributes AnnotationDefault,
                EnclosingMethod,
                InnerClasses,
                Signature,
                Synthetic,
                Bridge
-keep class androidx.compose.ui.geometry.** { *; }
-keep class androidx.compose.foundation.** { *; }

# --- Keep data classes for JSON serialization ---
-keepclassmembers class com.dominickp.firetvandroidtv.data.model.** {
    <init>(...);
}
-keepclassmembers class com.dominickp.firetvandroidtv.config.** {
    <init>(...);
}
-keepclassmembers class com.dominickp.firetvandroidtv.analytics.** {
    <init>(...);
}

# --- Reflection ---
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}
