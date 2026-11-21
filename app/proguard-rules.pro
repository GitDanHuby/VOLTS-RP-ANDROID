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

# Preserve Java classes and method names referenced directly by exported JNI symbols.
-keep class com.voltsrp.mobile.SAMP { *; }
-keep class com.voltsrp.mobile.ui.AttachEdit { *; }
-keep class com.voltsrp.mobile.ui.dialog.DialogManager { *; }
-keep class com.voltsrp.mobile.oswrapper.GameNative { *; }
-keep class com.voltsrp.mobile.gtasa.GameActivity { *; }

# Keep the legacy-package JNI facade and its native method names.
-keep class com.rockstargames.oswrapper.GameNative { *; }

# JNI resolves this legacy class by its exact binary name and field names.
-keep class com.rockstargames.oswrapper.DeviceInfo { *; }

# Preserve the exact legacy class/method names looked up by libGame.so.
-keep class com.rockstargames.oswrapper.GamePlatformServices { *; }

# JNI in libGame.so resolves these inherited methods by their original names.
-keep class com.voltsrp.mobile.oswrapper.GamePlatformServices { *; }
