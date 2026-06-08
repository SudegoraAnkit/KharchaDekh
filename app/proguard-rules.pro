# Add project specific ProGuard rules here.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Room database entities and DAOs to preserve reflection mapping
-keep class com.ankitsudegora.data.** { *; }

# Keep the Notification Listener Service to ensure successful system binding
-keep class com.ankitsudegora.service.TransactionNotificationListener { *; }
