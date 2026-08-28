# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Google Play Services
-keep class com.google.android.gms.** { *; }

# Modelos de dados
-keep class com.automapoko.app.domain.model.** { *; }
-keep class com.automapoko.app.data.local.entity.** { *; }
