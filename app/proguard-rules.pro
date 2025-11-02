# Add project specific ProGuard rules here.
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for stack traces
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*

# --- Keep @Keep-annotated classes and members
-keep @androidx.annotation.Keep class * { *; }

# Keep entry points (scoped)
-keep class io.mrarm.irc.IRCApplication { *; }
-keep class io.mrarm.irc.MainActivity { *; }
-keep class io.mrarm.irc.IRCService { *; }
-keep class io.mrarm.irc.SettingsActivity { *; }
-keep class io.mrarm.irc.DCCActivity { *; }

# --- Fragments created reflectively
-keep class io.mrarm.irc.chat.** extends androidx.fragment.app.Fragment { *; }
-keep class io.mrarm.irc.setting.** extends androidx.fragment.app.Fragment { *; }


# --- YAML-generated configuration classes
-keep class io.mrarm.irc.config.** { *; }

# --- Views inflated dynamically via Class.forName(...)
-keep class io.mrarm.irc.view.** {
    <init>(android.content.Context, android.util.AttributeSet);
}

# RecyclerView ViewHolder constructors
-keep public class * extends io.mrarm.irc.util.EntryRecyclerViewAdapter$EntryHolder {
    <init>(...);
}

# --- Keep classes with native methods
-keepclasseswithmembers class * {
    native <methods>;
}

# --- Preserve resource references used by reflection
-keepclassmembers class **.R$* {
    public static <fields>;
}

# --- Gson reflection (JSON serialization/deserialization)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# --- Suppress harmless warnings
-dontwarn org.spongycastle.**
-dontwarn org.junit.**
-dontwarn android.test.**
