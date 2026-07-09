# Keep Supabase / Ktor / kotlinx.serialization models
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.litsorbeklik.app.**$$serializer { *; }
-keepclassmembers class com.litsorbeklik.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.litsorbeklik.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
