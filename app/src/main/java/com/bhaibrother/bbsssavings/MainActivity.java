package com.bhaibrother.bbsssavings;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String HOME_URL = "https://bhai-brother.github.io/bbss-savings/bbss_savings_pro.html";
    private static final String INTERNAL_HOST = "bhai-brother.github.io";
    private static final String INTERNAL_PATH = "/bbss-savings/";
    private static final String USER_AGENT_SUFFIX = " BBSSAndroid/2.0";

    private static final int FILE_CHOOSER_REQUEST = 5173;
    private static final int SAVE_FILE_REQUEST = 5174;
    private static final int MAX_NATIVE_DOWNLOAD_BYTES = 20 * 1024 * 1024;

    private WebView webView;
    private ProgressBar progressBar;
    private ValueCallback<Uri[]> filePathCallback;
    private PendingFile pendingFile;
    private boolean showingOfflinePage = false;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.parseColor("#0D120F"));
        getWindow().setNavigationBarColor(Color.parseColor("#0D120F"));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#0D120F"));
        applySystemBarInsets(root);

        webView = new WebView(this);
        FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        root.addView(webView, webParams);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.getProgressDrawable().setTint(Color.parseColor("#55C58F"));
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(3)
        );
        progressParams.gravity = Gravity.TOP;
        root.addView(progressBar, progressParams);

        setContentView(root);
        configureWebView();
        configureBackNavigation();
        registerNetworkMonitor();

        if (savedInstanceState != null && webView.restoreState(savedInstanceState) != null) {
            return;
        }
        loadHome();
    }

    private void applySystemBarInsets(View root) {
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()
                );
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                view.setPadding(
                        insets.getSystemWindowInsetLeft(),
                        insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),
                        insets.getSystemWindowInsetBottom()
                );
            }
            return insets;
        });
    }

    private void configureBackNavigation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    this::handleBackNavigation
            );
        }
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setTextZoom(100);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setGeolocationEnabled(false);
        settings.setUserAgentString(settings.getUserAgentString() + USER_AGENT_SUFFIX);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        boolean debugBuild = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        WebView.setWebContentsDebuggingEnabled(debugBuild);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, false);
        }

        webView.addJavascriptInterface(new NativeBridge(), "BBSSNative");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                CookieManager.getInstance().flush();
                if (isInternalHttpUrl(url)) {
                    showingOfflinePage = false;
                    injectAndroidEnhancements();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    showOfflinePage();
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                if (request.isForMainFrame() && errorResponse != null && errorResponse.getStatusCode() >= 400) {
                    showOfflinePage();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(
                    WebView source,
                    ValueCallback<Uri[]> newCallback,
                    FileChooserParams fileChooserParams
            ) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = newCallback;

                Intent chooserIntent;
                try {
                    Intent contentIntent = fileChooserParams != null
                            ? fileChooserParams.createIntent()
                            : defaultFilePickerIntent();
                    chooserIntent = Intent.createChooser(contentIntent, "ছবি/ফাইল নির্বাচন করুন");
                    startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "ফাইল নির্বাচন করার অ্যাপ পাওয়া যায়নি", Toast.LENGTH_LONG).show();
                    return false;
                }
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // The current BBSS website does not require camera/mic/location access.
                request.deny();
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            if (url != null && url.startsWith("blob:")) {
                downloadBlobUrl(url, fileName, mimeType);
                return;
            }
            if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                openExternal(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return;
            }
            Toast.makeText(this, "এই download link app থেকে সংরক্ষণ করা যায়নি", Toast.LENGTH_SHORT).show();
        });
    }

    private Intent defaultFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        return intent;
    }

    private void injectAndroidEnhancements() {
        String script = "(function(){"
                + "if(!window.BBSSNative||window.__bbssAndroidV2)return;"
                + "window.__bbssAndroidV2=true;"
                + "function b64Utf8(text){"
                + "var bytes=new TextEncoder().encode(String(text));"
                + "var binary='';var step=32768;"
                + "for(var i=0;i<bytes.length;i+=step){binary+=String.fromCharCode.apply(null,bytes.subarray(i,Math.min(i+step,bytes.length)));}"
                + "return btoa(binary);"
                + "}"
                + "window.download=function(text,name,type){"
                + "try{BBSSNative.saveBase64File(String(name||'BBSS-download.txt'),String(type||'application/octet-stream'),b64Utf8(text));}"
                + "catch(e){console.error('BBSS native download failed',e);}"
                + "};"
                + "window.print=function(){try{BBSSNative.printPage(document.title||'BBSS Savings');}catch(e){console.error(e);}};"
                + "document.addEventListener('click',function(e){"
                + "var a=e.target&&e.target.closest?e.target.closest('a[target=\\\"_blank\\\"]'):null;"
                + "if(a&&a.href){e.preventDefault();window.location.href=a.href;}"
                + "},true);"
                + "})();";
        webView.evaluateJavascript(script, null);
    }

    private void downloadBlobUrl(String blobUrl, String fileName, String mimeType) {
        String safeName = sanitizeFileName(fileName, "BBSS-download");
        String safeMime = mimeType == null || mimeType.trim().isEmpty() ? "application/octet-stream" : mimeType;
        String js = "(async function(){try{"
                + "const r=await fetch(" + JSONObject.quote(blobUrl) + ");"
                + "const b=await r.blob();const ab=await b.arrayBuffer();const bytes=new Uint8Array(ab);"
                + "let binary='';const step=32768;"
                + "for(let i=0;i<bytes.length;i+=step){binary+=String.fromCharCode.apply(null,bytes.subarray(i,Math.min(i+step,bytes.length)));}"
                + "BBSSNative.saveBase64File(" + JSONObject.quote(safeName) + ","
                + JSONObject.quote(safeMime) + ",btoa(binary));"
                + "}catch(e){console.error(e);}})();";
        webView.evaluateJavascript(js, null);
    }

    private boolean handleUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.US);

        if ("http".equals(scheme) || "https".equals(scheme)) {
            if (isInternalUri(uri)) {
                return false;
            }
            openExternal(new Intent(Intent.ACTION_VIEW, uri));
            return true;
        }

        if ("intent".equals(scheme)) {
            try {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    String fallback = intent.getStringExtra("browser_fallback_url");
                    if (fallback != null && !fallback.isEmpty()) {
                        openExternal(new Intent(Intent.ACTION_VIEW, Uri.parse(fallback)));
                    } else if (intent.getPackage() != null) {
                        openExternal(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + intent.getPackage())));
                    }
                }
            } catch (URISyntaxException e) {
                Toast.makeText(this, "এই app link খোলা যায়নি", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if ("tel".equals(scheme) || "mailto".equals(scheme) || "sms".equals(scheme)
                || "smsto".equals(scheme) || "market".equals(scheme) || "geo".equals(scheme)
                || "bkash".equals(scheme) || "nagad".equals(scheme)) {
            openExternal(new Intent(Intent.ACTION_VIEW, uri));
            return true;
        }

        return false;
    }

    private boolean isInternalHttpUrl(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        return ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) && isInternalUri(uri);
    }

    private boolean isInternalUri(Uri uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.US);
        String path = uri.getPath() == null ? "/" : uri.getPath();
        return INTERNAL_HOST.equals(host) && path.startsWith(INTERNAL_PATH);
    }

    private void openExternal(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "এই লিংক খোলার উপযুক্ত অ্যাপ পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadHome() {
        showingOfflinePage = false;
        webView.loadUrl(HOME_URL);
    }

    private void showOfflinePage() {
        if (isFinishing() || webView == null) return;
        showingOfflinePage = true;
        String html = "<!doctype html><html lang='bn'><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>body{margin:0;background:#0d120f;color:#f4f3eb;font-family:sans-serif;display:grid;place-items:center;min-height:100vh;padding:24px;box-sizing:border-box}"
                + ".card{max-width:420px;text-align:center;background:#141c17;border:1px solid #2c3a31;border-radius:18px;padding:26px;box-shadow:0 18px 50px rgba(0,0,0,.28)}"
                + "h2{color:#55c58f}p{color:#aab4ae;line-height:1.6}a{display:inline-block;margin-top:10px;padding:12px 18px;border-radius:10px;background:#2f8e63;color:white;text-decoration:none;font-weight:700}</style></head>"
                + "<body><div class='card'><h2>ইন্টারনেট সংযোগ নেই</h2><p>BBSS Online Database খুলতে ইন্টারনেট প্রয়োজন। সংযোগ ঠিক হলে অ্যাপ স্বয়ংক্রিয়ভাবে আবার চেষ্টা করবে।</p>"
                + "<a href='" + HOME_URL + "'>এখনই আবার চেষ্টা করুন</a></div></body></html>";
        webView.loadDataWithBaseURL("https://offline.bbss.local/", html, "text/html", "UTF-8", null);
    }

    private void registerNetworkMonitor() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if (!showingOfflinePage) return;
                runOnUiThread(() -> {
                    if (showingOfflinePage && webView != null) {
                        loadHome();
                    }
                });
            }
        };

        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        } catch (Exception ignored) {
            networkCallback = null;
        }
    }

    private void printCurrentPage(String requestedName) {
        if (webView == null) return;
        String jobName = sanitizeFileName(requestedName, "BBSS Savings");
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        if (printManager == null) {
            Toast.makeText(this, "Print service পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            return;
        }
        PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter(jobName);
        printManager.print(jobName, adapter, new PrintAttributes.Builder().build());
    }

    private void requestSaveFile(String fileName, String mimeType, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            Toast.makeText(this, "খালি ফাইল সংরক্ষণ করা হয়নি", Toast.LENGTH_SHORT).show();
            return;
        }
        if (bytes.length > MAX_NATIVE_DOWNLOAD_BYTES) {
            Toast.makeText(this, "ফাইলটি app download limit-এর চেয়ে বড়", Toast.LENGTH_LONG).show();
            return;
        }

        String safeName = sanitizeFileName(fileName, "BBSS-download");
        String safeMime = mimeType == null || mimeType.trim().isEmpty()
                ? "application/octet-stream"
                : mimeType.split(";", 2)[0].trim();

        pendingFile = new PendingFile(safeName, safeMime, bytes);
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(safeMime);
        intent.putExtra(Intent.EXTRA_TITLE, safeName);

        try {
            startActivityForResult(intent, SAVE_FILE_REQUEST);
        } catch (ActivityNotFoundException e) {
            pendingFile = null;
            Toast.makeText(this, "ফাইল সংরক্ষণের জন্য Files app পাওয়া যায়নি", Toast.LENGTH_LONG).show();
        }
    }

    private void savePendingFile(Uri uri) {
        PendingFile file = pendingFile;
        pendingFile = null;
        if (file == null || uri == null) return;

        try (OutputStream output = getContentResolver().openOutputStream(uri, "w")) {
            if (output == null) throw new IllegalStateException("Output stream unavailable");
            output.write(file.bytes);
            output.flush();
            Toast.makeText(this, file.name + " সংরক্ষণ হয়েছে", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "ফাইল সংরক্ষণ ব্যর্থ: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String sanitizeFileName(String value, String fallback) {
        String name = value == null ? "" : value.trim();
        if (name.isEmpty()) name = fallback;
        name = name.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_");
        if (name.length() > 120) name = name.substring(0, 120);
        return name;
    }

    private void handleBackNavigation() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) return;
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
            return;
        }

        if (requestCode == SAVE_FILE_REQUEST) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                savePendingFile(data.getData());
            } else {
                pendingFile = null;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            handleBackNavigation();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {
            }
        }

        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        pendingFile = null;

        if (webView != null) {
            webView.stopLoading();
            webView.removeJavascriptInterface("BBSSNative");
            webView.setDownloadListener(null);
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private final class NativeBridge {
        @JavascriptInterface
        public void saveBase64File(String fileName, String mimeType, String base64Data) {
            if (base64Data == null || base64Data.length() > MAX_NATIVE_DOWNLOAD_BYTES * 2L) {
                runOnUiThread(() -> Toast.makeText(
                        MainActivity.this,
                        "Download data খুব বড় বা invalid",
                        Toast.LENGTH_LONG
                ).show());
                return;
            }

            try {
                byte[] decoded = Base64.decode(base64Data, Base64.DEFAULT);
                runOnUiThread(() -> requestSaveFile(fileName, mimeType, decoded));
            } catch (IllegalArgumentException e) {
                runOnUiThread(() -> Toast.makeText(
                        MainActivity.this,
                        "Download data decode করা যায়নি",
                        Toast.LENGTH_LONG
                ).show());
            }
        }

        @JavascriptInterface
        public void printPage(String title) {
            runOnUiThread(() -> printCurrentPage(title));
        }
    }

    private static final class PendingFile {
        final String name;
        final String mimeType;
        final byte[] bytes;

        PendingFile(String name, String mimeType, byte[] bytes) {
            this.name = name;
            this.mimeType = mimeType;
            this.bytes = bytes;
        }
    }
}
