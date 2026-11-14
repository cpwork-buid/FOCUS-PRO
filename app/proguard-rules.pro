-keep class com.focuspro.app.data.local.entity.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>();
}
