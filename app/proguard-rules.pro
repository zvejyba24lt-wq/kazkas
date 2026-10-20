# kotlinx.serialization ships its own rules; keep our serializable models' companions explicitly.
-keepclassmembers @kotlinx.serialization.Serializable class lt.marcomultiservices.kimba.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class lt.marcomultiservices.kimba.**$$serializer { *; }
