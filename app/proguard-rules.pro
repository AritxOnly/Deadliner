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

# --- 关键：保留泛型签名 & 注解 ---
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# --- Gson & TypeToken ---
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class ** extends com.google.gson.reflect.TypeToken { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# （可选）如果你用反射注册适配器/工厂，也保留它们（按你项目包名改）
# -keep class com.aritxonly.deadliner.** implements com.google.gson.TypeAdapterFactory { *; }
# -keep class com.aritxonly.deadliner.** implements com.google.gson.JsonSerializer { *; }
# -keep class com.aritxonly.deadliner.** implements com.google.gson.JsonDeserializer { *; }