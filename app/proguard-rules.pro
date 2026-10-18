# kotlinx.serialization ships its own rules; keep the content models' serializers explicitly.
-keepclassmembers @kotlinx.serialization.Serializable class lt.marcomultiservices.skaitaupats.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class lt.marcomultiservices.skaitaupats.**$$serializer { *; }
