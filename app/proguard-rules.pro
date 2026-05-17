# ============================================
# Compose
# ============================================
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.material.icons.** { *; }
-keep class androidx.compose.animation.** { *; }
-keep class androidx.compose.foundation.** { *; }
-dontwarn androidx.compose.**

# ============================================
# Activity & ViewModel
# ============================================
-keep class * extends androidx.activity.ComponentActivity
-keep class * extends androidx.lifecycle.ViewModel
-keep class * extends androidx.lifecycle.AndroidViewModel
-keepclassmembers class * extends androidx.lifecycle.ViewModel { <methods>; }
-dontwarn androidx.lifecycle.viewmodel.compose.**

# ============================================
# Navigation
# ============================================
-keep class androidx.navigation.** { *; }
-keep class * extends androidx.navigation.NavType { *; }
-dontwarn androidx.navigation.**

# ============================================
# DataStore
# ============================================
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ============================================
# Shizuku
# ============================================
-keep class rikka.shizuku.** { *; }
-keep class dev.rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# ============================================
# App & Service classes
# ============================================
-keep class beta.manager.App { *; }
-keep class beta.manager.ui.MainActivity { *; }
-keep class beta.manager.ui.CrashReportActivity { *; }
-keep class beta.manager.service.BetaService { *; }
-keep class beta.manager.service.BootReceiver { *; }
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver

# ============================================
# Plugin & Utils (keep all for dynamic loading)
# ============================================
-keep class beta.manager.plugin.** { *; }
-keep class beta.manager.utils.** { *; }
-keep class beta.manager.adb.** { *; }
-keep class beta.manager.ui.viewmodel.** { *; }
-keep class beta.manager.ui.screen.** { *; }
-keep class beta.manager.ui.component.** { *; }
-keep class beta.manager.ui.theme.** { *; }
-keep class beta.manager.navigation.** { *; }

# ============================================
# AndroidX & Kotlin
# ============================================
-keep class androidx.core.** { *; }
-keep class androidx.appcompat.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-dontwarn org.jetbrains.annotations.**

# ============================================
# Keep native methods & enums
# ============================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Resources are kept by default by R8/AGP
