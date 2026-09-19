# Dumbify — release shrinking rules.
# Compose, AndroidX and Hilt ship their own consumer rules; keep only what the
# app itself needs reflection for.

# kotlinx.serialization keeps the generated serializers reachable.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.werkloop.dumbify.** {
    *** Companion;
}
-keepclasseswithmembers class com.werkloop.dumbify.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Navigation 3 resolves NavKey types by name when restoring a saved back stack.
-keep,includedescriptorclasses class com.werkloop.dumbify.ui.nav.** { *; }
