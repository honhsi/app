package com.hs.club;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    public static final String EXTRA_URL = "extra_url";
    public static final String EXTRA_GAME = "extra_game";
    public static final String EXTRA_VENDOR = "extra_vendor";
    public static final String EXTRA_SKIP_SPLASH = "extra_skip_splash";
    private WebView webView;
    private FrameLayout root;
    private TextView nativeBack;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean gameActive = false;
    private boolean pageReady = false;
    private boolean directGame = false;
    private FrameLayout splashOverlay;
    private FrameLayout errorOverlay;
    private String startUrl = "";
    private long splashStartedAt = 0;
    private long splashMinMs = 2200;
    private boolean pageLoaded = false;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 1002;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setImmersive();

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#f8f5ff"));
        setContentView(root);

        String launchUrl = getIntent().getStringExtra(EXTRA_URL);
        final boolean isGame = getIntent().getBooleanExtra(EXTRA_GAME, false);
        directGame = isGame;
        if (launchUrl == null || launchUrl.trim().isEmpty()) launchUrl = BuildConfig.INITIAL_URL;
        startUrl = launchUrl;

        webView = new WebView(this);
        FrameLayout.LayoutParams webLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        root.addView(webView, webLp);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setSaveFormData(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        String ua = s.getUserAgentString();
        if (ua != null && !ua.contains("HSClubApp")) {
            s.setUserAgentString(ua + " HSClubApp/1.0");
        }

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setBackgroundColor(Color.parseColor("#f8f5ff"));
        if (Build.VERSION.SDK_INT >= 26) {
            webView.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        }

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true);

        webView.addJavascriptInterface(new Bridge(), "HSApp");
        webView.setWebViewClient(new Client());
        webView.setWebChromeClient(new Chrome());
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception ignored) {}
        });

        createNativeBack();
        splashMinMs = getIntent().getBooleanExtra(EXTRA_SKIP_SPLASH, false) ? 300 : 2200;
        if (!isGame) createSplash();
        webView.loadUrl(startUrl);
        if (isGame) setGameActive(true);
        if (!isGame) UpdateActivity.check(this);
    }

    private void createSplash() {
        if (splashOverlay != null || errorOverlay != null) return;
        splashStartedAt = System.currentTimeMillis();
        splashOverlay = new FrameLayout(this);
        splashOverlay.setBackgroundColor(Color.parseColor("#f8f5ff"));
        ImageView bg = new ImageView(this);
        bg.setImageResource(R.drawable.splash);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        splashOverlay.addView(bg, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView tip = new TextView(this);
        tip.setText("正在连接最优线路...");
        tip.setTextSize(13);
        tip.setTextColor(Color.parseColor("#7c6cff"));
        tip.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(36));
        tlp.gravity = Gravity.BOTTOM;
        tlp.bottomMargin = dp(54);
        splashOverlay.addView(tip, tlp);
        root.addView(splashOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        handler.postDelayed(() -> {
            if (!pageLoaded) showLoadError();
        }, 12000);
    }

    private void finishSplash() {
        if (splashOverlay == null) return;
        long elapsed = System.currentTimeMillis() - splashStartedAt;
        if (elapsed < splashMinMs) {
            handler.postDelayed(this::finishSplash, splashMinMs - elapsed + 30);
            return;
        }
        final FrameLayout old = splashOverlay;
        splashOverlay = null;
        old.animate().alpha(0f).setDuration(220).withEndAction(() -> {
            if (old.getParent() != null) root.removeView(old);
        }).start();
    }

    private void showLoadError() {
        if (errorOverlay != null) return;
        pageLoaded = false;
        if (splashOverlay != null) {
            root.removeView(splashOverlay);
            splashOverlay = null;
        }
        errorOverlay = new FrameLayout(this);
        errorOverlay.setBackgroundColor(Color.parseColor("#f8f5ff"));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(32), dp(80), dp(32), dp(40));

        TextView title = new TextView(this);
        title.setText("线路繁忙");
        title.setTextColor(Color.parseColor("#2b2042"));
        title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView desc = new TextView(this);
        desc.setText("暂时无法连接服务器，请检查网络后重试。");
        desc.setTextColor(Color.parseColor("#7b6d93"));
        desc.setTextSize(13);
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(0, dp(12), 0, dp(28));
        box.addView(desc);

        Button retry = new Button(this);
        retry.setText("重新连接");
        retry.setTextColor(Color.WHITE);
        retry.setBackground(roundBg("#7c6cff", 24));
        retry.setOnClickListener(v -> retryLoad());
        box.addView(retry, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        if (BuildConfig.OFFICIAL_URL != null && !BuildConfig.OFFICIAL_URL.isEmpty()) {
            Button official = new Button(this);
            official.setText("打开官网");
            official.setTextColor(Color.parseColor("#7c6cff"));
            official.setBackground(roundBg("#f1ecff", 24));
            official.setOnClickListener(v -> {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.OFFICIAL_URL))); } catch (Exception ignored) {}
            });
            LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
            olp.topMargin = dp(10);
            box.addView(official, olp);
        }

        errorOverlay.addView(box, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        root.addView(errorOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void retryLoad() {
        if (errorOverlay != null) {
            root.removeView(errorOverlay);
            errorOverlay = null;
        }
        pageLoaded = false;
        createSplash();
        webView.loadUrl(startUrl);
    }

    private void checkUpdate() {
        new Thread(() -> {
            try {
                String base = BuildConfig.INITIAL_URL.endsWith("/")
                        ? BuildConfig.INITIAL_URL.substring(0, BuildConfig.INITIAL_URL.length() - 1)
                        : BuildConfig.INITIAL_URL;
                java.net.URL url = new java.net.URL(base + "/app_config.php");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestProperty("Accept", "application/json");
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) return;
                java.io.InputStream is = conn.getInputStream();
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                is.close();
                org.json.JSONObject json = new org.json.JSONObject(bos.toString("UTF-8"));
                int remote = json.optInt("version_code", 0);
                if (remote <= BuildConfig.VERSION_CODE) return;
                String name = json.optString("version_name", "");
                String changelog = json.optString("changelog", "");
                String download = json.optString("download_url", "");
                if (download.isEmpty()) download = json.optString("official_url", BuildConfig.OFFICIAL_URL);
                final String dl = download;
                boolean force = json.optInt("force_update", 0) == 1;
                runOnUiThread(() -> {
                    AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this)
                            .setTitle("发现新版本 " + name)
                            .setMessage(changelog.isEmpty() ? "请下载最新版本" : changelog)
                            .setPositiveButton("去下载", (d, w) -> {
                                if (!dl.isEmpty()) {
                                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(dl))); } catch (Exception ignored) {}
                                }
                            });
                    if (!force) b.setNegativeButton("以后再说", null);
                    b.setCancelable(!force);
                    b.show();
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void createNativeBack() {
        nativeBack = new TextView(this);
        nativeBack.setText("←");
        nativeBack.setTextSize(20);
        nativeBack.setTextColor(Color.WHITE);
        nativeBack.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#66000000"));
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), Color.parseColor("#66e6c27a"));
        nativeBack.setBackground(bg);
        nativeBack.setElevation(dp(6));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(40), dp(40));
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.setMargins(dp(10), dp(10), 0, 0);
        nativeBack.setVisibility(View.GONE);
        nativeBack.setOnClickListener(v -> {
            nativeBack.animate().alpha(1f).setDuration(120).start();
            if (directGame) {
                showExitConfirm();
            } else {
                webView.evaluateJavascript("window.closeGame && window.closeGame()", null);
            }
        });
        root.addView(nativeBack, lp);
    }

    private void setGameActive(boolean active) {
        gameActive = active;
        if (nativeBack == null) return;
        if (!active) {
            nativeBack.setVisibility(View.GONE);
            return;
        }
        nativeBack.setVisibility(View.VISIBLE);
        nativeBack.setAlpha(1f);
        nativeBack.bringToFront();
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> {
            if (nativeBack != null && gameActive) nativeBack.animate().alpha(0.55f).setDuration(350).start();
        }, 4000);
    }

    private void injectAppCSS() {
        if (!directGame) return;
        String js = "(function(){" +
                "var st=document.getElementById('hs-app-style');" +
                "if(!st){st=document.createElement('style');st.id='hs-app-style';" +
                "st.textContent='#hsQuickFab,#hsQuickPanel,.game-header{display:none!important}' ;" +
                "document.head.appendChild(st);}" +
                "if(!window.__hsAppObserverInit){window.__hsAppObserverInit=true;" +
                "var report=function(){var f=document.getElementById('game-frame-container');" +
                "var on=!!(f&&(f.classList.contains('game-visible')||f.classList.contains('game-fullscreen')||f.style.display==='flex'));" +
                "try{HSApp.setGameActive(on);}catch(e){}};" +
                "var f=document.getElementById('game-frame-container');" +
                "if(f)new MutationObserver(report).observe(f,{attributes:true,attributeFilter:['class','style']});" +
                "report();}" +
                "})()";
        webView.evaluateJavascript(js, null);
    }

    private boolean isPaymentHost(String host) {
        if (host == null || host.isEmpty()) return false;
        String h = host.toLowerCase();
        String raw = BuildConfig.PAYMENT_HOSTS;
        if (raw != null && !raw.trim().isEmpty()) {
            for (String item : raw.split(",")) {
                String t = item.trim().toLowerCase();
                if (!t.isEmpty() && (h.equals(t) || h.endsWith("." + t))) return true;
            }
        }
        // 支付/收银台常见域名，即使后台还没填全也走外部浏览器
        String[] defaults = {
                "alipay.com", "alipays.com", "weixin.qq.com", "tenpay.com",
                "unionpay.com", "95516.com", "paypal.com", "stripe.com"
        };
        for (String t : defaults) {
            if (h.equals(t) || h.endsWith("." + t)) return true;
        }
        return false;
    }

    private boolean openExternal(Uri uri, String failMessage) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(intent);
            return true;
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, failMessage, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private class Client extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            if (uri.getPath() != null && uri.getPath().startsWith("/pay_bridge.php")) {
                openExternal(uri, "未找到可用浏览器，请安装浏览器后重试");
                return true;
            }
            if ("http".equals(scheme) || "https".equals(scheme)) {
                if (isPaymentHost(uri.getHost())) {
                    openExternal(uri, "未找到可用浏览器，请安装浏览器后重试");
                    return true;
                }
                return false;
            }
            if ("intent".equals(scheme)) {
                try {
                    Intent intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        String fallback = intent.getStringExtra("browser_fallback_url");
                        if (fallback != null && !fallback.isEmpty()) {
                            openExternal(Uri.parse(fallback), "无法打开支付页面");
                        } else {
                            Toast.makeText(MainActivity.this, "未安装对应的支付应用", Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "无法打开该支付链接", Toast.LENGTH_LONG).show();
                }
                return true;
            }
            if ("tel".equals(scheme) || "mailto".equals(scheme)) {
                openExternal(uri, "无法打开该应用");
                return true;
            }
            if ("about".equals(scheme) || "data".equals(scheme) || "blob".equals(scheme) || "javascript".equals(scheme)) {
                return false;
            }
            // alipays:// / weixin:// / upi:// 等支付 deep link 交给系统 App
            String fail = scheme.startsWith("alipay") ? "未安装支付宝，请先安装支付宝后重试"
                    : scheme.startsWith("weixin") || scheme.startsWith("wxpay") ? "未安装微信，请先安装微信后重试"
                    : "未找到可处理该链接的应用";
            openExternal(uri, fail);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            pageReady = true;
            pageLoaded = true;
            finishSplash();
            injectAppCSS();
            if (!directGame) {
                injectCredentialAutofill();
                if (url != null && url.contains("kicked=1")) showKickedDialog();
            }
            if (directGame) injectGameBack();
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            if (!directGame && !pageLoaded) {
                showLoadError();
            }
        }
    }

    private class Chrome extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress == 100) injectAppCSS();
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(null);
            }
            filePathCallback = callback;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            String[] accept = params != null ? params.getAcceptTypes() : null;
            if (accept != null && accept.length > 0) {
                for (String a : accept) {
                    if (a != null && !a.trim().isEmpty() && !a.contains("image")) {
                        intent.setType("*/*");
                        break;
                    }
                }
            }
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,
                    params != null && params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
            try {
                startActivityForResult(Intent.createChooser(intent, "选择图片"), FILE_CHOOSER_REQUEST);
            } catch (ActivityNotFoundException e) {
                filePathCallback = null;
                Toast.makeText(MainActivity.this, "未找到文件选择器", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                ClipData clip = data.getClipData();
                if (clip != null) {
                    results = new Uri[clip.getItemCount()];
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        results[i] = clip.getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
        }
    }

    private void injectCredentialAutofill() {
        String js = "(function(){" +
                "var cred=null;try{var raw=HSApp.loadLogin();if(raw)cred=JSON.parse(raw);}catch(e){}" +
                "var rem=document.getElementById('authRemember');" +
                "if(cred&&cred.u){var u=document.querySelector('#authLoginForm input[name=username]');var p=document.querySelector('#authLoginForm input[name=password]');if(u&&!u.value)u.value=cred.u;if(p&&!p.value&&cred.p)p.value=cred.p;if(rem)rem.checked=true;}" +
                "if(!window.__hsCredInit){window.__hsCredInit=true;document.addEventListener('submit',function(e){var f=e.target;if(!f||f.id!=='authLoginForm')return;try{var u=f.querySelector('input[name=username]');var p=f.querySelector('input[name=password]');var r=f.querySelector('#authRemember');if(u&&p&&u.value&&p.value){if(r&&!r.checked)HSApp.clearLogin();else HSApp.saveLogin(u.value,p.value);}}catch(x){}},true);}" +
                "})()";
        webView.evaluateJavascript(js, null);
    }

    private void showKickedDialog() {
        if (isFinishing()) return;
        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle("登录已失效")
                    .setMessage("账号已在其他设备登录，当前设备已下线。")
                    .setCancelable(false)
                    .setPositiveButton("重新登录", (d, w) -> {
                        try {
                            CookieManager.getInstance().removeAllCookies(null);
                            CookieManager.getInstance().flush();
                        } catch (Throwable ignored) {}
                        webView.loadUrl(BuildConfig.INITIAL_URL);
                    })
                    .setNegativeButton("退出", (d, w) -> finish())
                    .show();
        });
    }

    private void injectGameBack() {        String js = "(function(){" +
                "if(document.getElementById('hs-app-back'))return;" +
                "var b=document.createElement('div');b.id='hs-app-back';b.textContent='←';" +
                "b.style.cssText='position:fixed;top:calc(10px + env(safe-area-inset-top,0px));left:10px;width:36px;height:36px;border-radius:50%;z-index:2147483647;background:rgba(0,0,0,.55);border:1px solid rgba(230,194,122,.7);color:#fff;font:22px/34px sans-serif;text-align:center;cursor:pointer;transition:opacity .3s';" +
                "b.onclick=function(){try{HSApp.closeGame();}catch(e){}};" +
                "document.body.appendChild(b);" +
                "setTimeout(function(){b.style.opacity='.55';},4000);" +
                "b.addEventListener('touchstart',function(){b.style.opacity='1';},{passive:true});" +
                "})()";
        webView.evaluateJavascript(js, null);
    }

    private class Bridge {
        @JavascriptInterface
        public void setGameActive(final boolean active) {
            runOnUiThread(() -> MainActivity.this.setGameActive(active));
        }

        @JavascriptInterface
        public void closeGame() {
            runOnUiThread(MainActivity.this::showExitConfirm);
        }

        @JavascriptInterface
        public void saveLogin(String username, String password) {
            CredentialStore.save(MainActivity.this, username, password);
        }

        @JavascriptInterface
        public String loadLogin() {
            return CredentialStore.load(MainActivity.this);
        }

        @JavascriptInterface
        public void clearLogin() {
            CredentialStore.clear(MainActivity.this);
        }

        @JavascriptInterface
        public void openExternal(String url) {
            if (url == null || url.isEmpty()) return;
            runOnUiThread(() -> MainActivity.this.openExternal(Uri.parse(url), "未找到可用浏览器，请安装浏览器后重试"));
        }
    }

    private void setImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) setImmersive();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setImmersive();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        try { CookieManager.getInstance().flush(); } catch (Throwable ignored) {}
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (directGame) {
            showExitConfirm();
            return;
        }
        if (gameActive) {
            webView.evaluateJavascript("window.closeGame && window.closeGame()", null);
            return;
        }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("退出")
                .setMessage("确定退出 App 吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("退出", (d, w) -> finish())
                .show();
    }

    private void showExitConfirm() {
        new AlertDialog.Builder(this)
                .setTitle("退出游戏")
                .setMessage("确定返回游戏大厅吗？")
                .setNegativeButton("继续游戏", (d, w) -> {
                    if (nativeBack != null) nativeBack.animate().alpha(1f).setDuration(120).start();
                })
                .setPositiveButton("返回大厅", (d, w) -> finish())
                .show();
    }

    private GradientDrawable roundBg(String color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.parseColor(color));
        d.setCornerRadius(dp(radius));
        return d;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
