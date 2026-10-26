# Proguard rules for Khabar

# Stack traces readable rakhne ke liye
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# --- Room ---
-keepclassmembers class * {
    @androidx.room.* *;
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.**

# --- WorkManager: KhabarApp.MailSyncWorker reflection se banta hai ---
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**
-keep class com.khabar.app.KhabarApp$MailSyncWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- App ke data models (Room entities, domain models) ---
-keep class com.khabar.app.data.local.** { *; }
-keep class com.khabar.app.domain.model.** { *; }

# --- kotlinx.serialization (Json.parseToJsonElement / List<String> encode-decode) ---
-dontwarn kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- OkHttp / Okio ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Microsoft MSAL (Outlook login) ---
-keep class com.microsoft.identity.** { *; }
-dontwarn com.microsoft.identity.**
-dontwarn com.nimbusds.**
-dontwarn org.slf4j.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn com.yubico.**

# --- Google Play Services Auth / ML Kit (apne consumer rules ke saath aate hain) ---
-dontwarn com.google.android.gms.**
-dontwarn com.google.mlkit.**

# --- Misc ---
-dontwarn java.beans.**
-dontwarn javax.naming.**
