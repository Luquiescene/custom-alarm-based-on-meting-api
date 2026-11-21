# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Room
-keep class * extends androidx.room.RoomDatabase
