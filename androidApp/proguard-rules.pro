# R8 / ProGuard rules of the release build (see buildTypes.release in build.gradle.kts).
# The libraries of this app ship their own consumer rules (kotlinx.serialization, Compose, Coil, Firebase, Health
# Connect, ...). Only what they cannot know about belongs here.

# Keep line numbers in stack traces; the mapping.txt of the build translates them back.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room (KMP): the database and its generated constructor are created by reflection.
-keep class * extends androidx.room3.RoomDatabase { <init>(); }
-keep class * implements androidx.room3.RoomDatabaseConstructor { *; }

# Koin resolves the view models and use cases by their class; the shared module declares them in code, so nothing is
# reflected, but the qualifier object of the recording instance must survive.
-keep class com.wandr.di.** { *; }

# Supabase / Ktor reference JVM classes that do not exist on Android.
-dontwarn java.lang.management.**
-dontwarn org.slf4j.**
-dontwarn io.ktor.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
