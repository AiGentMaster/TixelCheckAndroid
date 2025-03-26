# Add project specific ProGuard rules here

# Keep the application class and its members
-keep class com.example.tixelcheck.** { *; }

# Keep all model classes
-keep class com.example.tixelcheck.MonitoredUrl { *; }

# JSoup rules
-keep public class org.jsoup.** {
    public *;
}

# Timber rules
-dontwarn org.jetbrains.annotations.**
-keep class timber.log.Timber { *; }

# AndroidX rules
-keep class androidx.appcompat.widget.** { *; }
-keep class androidx.appcompat.app.** { *; }

# General Android rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# Prevent obfuscation of classes with native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom Views that might be inflated from XML
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);;
}

# Keep BroadcastReceivers that are registered in the AndroidManifest
-keep class com.example.tixelcheck.BootReceiver
-keep class com.example.tixelcheck.NotificationActionReceiver
-keep class com.example.tixelcheck.TicketCheckerAlarm

# Keep Services
-keep class com.example.tixelcheck.TicketMonitorService

# Keep Activity classes that are referenced from XML layouts
-keep public class * extends android.app.Activity
-keep public class * extends androidx.appcompat.app.AppCompatActivity

# Keep any classes referenced from XML layouts
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Preserve all native method names and the names of their classes
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve all classes that have special context
-keepclassmembers class * extends android.content.Context {
   public void *(android.view.View);
   public void *(android.view.MenuItem);
}
