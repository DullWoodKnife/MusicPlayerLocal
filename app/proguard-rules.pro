# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Media3 classes
-keep class androidx.media3.** { *; }

# Keep Room entities
-keep class com.purebeat.data.local.** { *; }
