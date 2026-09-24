# JavaScript bridge methods are invoked by WebView JavaScript.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
