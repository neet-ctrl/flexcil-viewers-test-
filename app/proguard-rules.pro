-keep class com.flexcilviewer.data.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn kotlin.**

# PDFBox Android
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
