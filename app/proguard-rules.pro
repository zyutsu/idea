
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations, AnnotationDefault
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class dev.zyutsu.idea.**$$serializer { *; }
-keepclassmembers class dev.zyutsu.idea.** { *** Companion; }
-keepclasseswithmembers class dev.zyutsu.idea.** { kotlinx.serialization.KSerializer serializer(...); }
-dontwarn org.slf4j.**

