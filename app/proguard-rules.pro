# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# R8 missing rules (AGP-generated)
-dontwarn java.lang.reflect.AnnotatedType

# Keep data classes used across processes (UI → Provider IPC)
-keep class com.op.aod.enhance.data.** { *; }
-keep class com.op.aod.enhance.hook.AodConfig { *; }

# Keep HookEntry for YukiHookAPI/KSP generated entry point
-keep class com.op.aod.enhance.HookEntry { *; }
-keep class com.op.aod.enhance.HookEntryXposed { *; }