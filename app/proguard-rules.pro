# NOVA MUSIC R8 / ProGuard Configuration

# Preserve readable stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Kotlin enum classes used with valueOf(...) in PreferencesRepository & models
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep class com.novamusic.player.data.model.** { *; }

# Media3 ExoPlayer & MediaSessionService
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Room Database, Entities, DAOs
-keep class com.novamusic.player.database.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# AndroidX DataStore Preferences & Protobuf lite
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn androidx.datastore.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Coil Image Loading
-keep class coil.** { *; }
-dontwarn coil.**

# In-App Updater
-keep class com.novamusic.player.update.** { *; }

# Home Screen Widgets
-keep class com.novamusic.player.widget.* extends android.appwidget.AppWidgetProvider { *; }

# Nova Wrapped Analytics & Models
-keep class com.novamusic.player.wrapped.** { *; }
