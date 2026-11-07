# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}

# SDK de Anthropic: usa Jackson y reflexión para serializar
-keep class com.anthropic.** { *; }
-dontwarn com.anthropic.**
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**
-dontwarn java.beans.**
-dontwarn org.slf4j.**
-dontwarn okhttp3.internal.**
# Referencias opcionales del SDK de Anthropic a clases que no existen en Android.
# Solo las usa la generación de esquemas JSON, que esta app no utiliza.
-dontwarn com.google.auto.value.AutoValue
-dontwarn java.lang.reflect.AnnotatedParameterizedType
-dontwarn java.lang.reflect.AnnotatedType
