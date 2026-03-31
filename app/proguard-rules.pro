# Add project specific ProGuard rules here.
-keep class com.financetracker.app.data.db.entity.** { *; }
-keep class com.financetracker.app.data.db.dao.** { *; }
-dontwarn com.sun.mail.**
-dontwarn javax.mail.**
-keep class com.sun.mail.** { *; }
-keep class javax.mail.** { *; }
-keep class javax.activation.** { *; }
-dontwarn org.apache.harmony.**
