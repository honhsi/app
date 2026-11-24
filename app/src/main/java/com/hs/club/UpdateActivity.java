package com.hs.club;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/** 官网 APK 下载 + App 内更新安装。 */
public class UpdateActivity extends Activity {
    public static final String EXTRA_URL = "update_url";
    public static final String EXTRA_NAME = "update_name";
    public static final String EXTRA_LOG = "update_log";
    public static final String EXTRA_FORCE = "update_force";
    private static final String FILE_NAME = "hs-update.apk";

    private final Handler ui = new Handler(Looper.getMainLooper());
    private DownloadManager dm;
    private long downloadId = -1;
    private ProgressBar progress;
    private TextView status;
    private TextView action;
    private String url;
    private String versionName;
    private String changelog;
    private boolean force;
    private boolean receiverOn;

    private final BroadcastReceiver done = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null || !DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id != downloadId) return;
            finishDownload();
        }
    };

    /** 检查版本；有新版本时打开本页。 */
    public static void check(Activity activity) {
        new Thread(() -> {
            try {
                JSONObject json = Api.getJson("/app_config.php");
                final int remote = json.optInt("version_code", 0);
                if (remote <= BuildConfig.VERSION_CODE) return;
                String u = json.optString("download_url", "").trim();
                final String url = u;
                final String name = json.optString("version_name", "");
                final String log = json.optString("changelog", "");
                final boolean force = json.optInt("force_update", 0) == 1;
                activity.runOnUiThread(() -> {
                    Intent i = new Intent(activity, UpdateActivity.class);
                    i.putExtra(EXTRA_URL, url);
                    i.putExtra(EXTRA_NAME, name);
                    i.putExtra(EXTRA_LOG, log);
                    i.putExtra(EXTRA_FORCE, force);
                    activity.startActivity(i);
                });
            } catch (Throwable ignored) {}
        }, "hs-update-check").start();
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HsTheme.init(this);
        HsTheme.apply(this);
        url = getIntent().getStringExtra(EXTRA_URL);
        versionName = getIntent().getStringExtra(EXTRA_NAME);
        changelog = getIntent().getStringExtra(EXTRA_LOG);
        force = getIntent().getBooleanExtra(EXTRA_FORCE, false);
        if (url == null) url = "";
        if (versionName == null) versionName = "";
        if (changelog == null) changelog = "";
        dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(HsTheme.bg);
        setContentView(root);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(16), dp(12), dp(16), dp(8));
        if (!force) {
            TextView back = text("‹", 28, HsTheme.a, false);
            back.setGravity(Gravity.CENTER);
            back.setOnClickListener(v -> finish());
            bar.addView(back, new LinearLayout.LayoutParams(dp(32), dp(40)));
        }
        bar.addView(text("App 更新", 18, HsTheme.text, true),
                new LinearLayout.LayoutParams(0, dp(40), 1));
        root.addView(bar);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), 0, dp(16), dp(14));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackground(HsTheme.round(HsTheme.panel, 18, this));
        body.addView(card);
        card.addView(text("发现新版本 " + versionName, 17, HsTheme.text, true));
        card.addView(text(force ? "本次为强制更新" : "建议更新到最新版本", 9,
                force ? HsTheme.gold : HsTheme.muted, false));

        TextView log = text(changelog.isEmpty() ? "修复问题并优化体验" : changelog, 10, HsTheme.muted, false);
        log.setLineSpacing(0, 1.5f);
        log.setPadding(0, dp(14), 0, dp(14));
        card.addView(log);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(0);
        progress.setVisibility(View.GONE);
        card.addView(progress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));

        status = text("点击下方按钮下载安装包", 9, HsTheme.muted, false);
        status.setPadding(0, dp(8), 0, 0);
        card.addView(status);

        action = text("立即更新", 13, Color.WHITE, true);
        action.setGravity(Gravity.CENTER);
        action.setBackground(HsTheme.gradient(14, this));
        action.setOnClickListener(v -> startDownload());
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        alp.topMargin = dp(14);
        body.addView(action, alp);

        TextView tip = text("更新包来自官网，安装前请允许“安装未知应用”。如已是最新版本，可忽略本次提示。", 8, HsTheme.muted, false);
        tip.setLineSpacing(0, 1.45f);
        tip.setPadding(0, dp(12), 0, 0);
        body.addView(tip);

        if (force) {
            TextView later = text("暂不更新", 9, HsTheme.muted, false);
            later.setGravity(Gravity.CENTER);
            later.setPadding(0, dp(12), 0, 0);
            later.setOnClickListener(v -> Toast.makeText(this, "本次更新为强制更新", Toast.LENGTH_SHORT).show());
            body.addView(later);
        }
    }

    private void startDownload() {
        if (url.isEmpty()) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Api.base() + "/download.php")));
            } catch (Throwable ignored) {}
            return;
        }
        if (!url.startsWith("http")) url = Api.base() + (url.startsWith("/") ? "" : "/") + url;
        // 只允许 https（或与 API 同主机的地址），避免明文下载被替换
        try {
            Uri u = Uri.parse(url);
            String scheme = u.getScheme() == null ? "" : u.getScheme().toLowerCase();
            String host = u.getHost() == null ? "" : u.getHost().toLowerCase();
            String apiHost = Uri.parse(Api.base()).getHost();
            apiHost = apiHost == null ? "" : apiHost.toLowerCase();
            if (!"https".equals(scheme) && !(host.equals(apiHost) && !host.isEmpty())) {
                status.setText("更新地址不是 HTTPS，已阻止下载");
                return;
            }
        } catch (Throwable ignored) {}
        try {
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
            req.setTitle("HS App 更新");
            req.setDescription("正在下载 " + versionName);
            req.setMimeType("application/vnd.android.package-archive");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, FILE_NAME);
            downloadId = dm.enqueue(req);
            progress.setVisibility(View.VISIBLE);
            action.setEnabled(false);
            action.setAlpha(0.6f);
            status.setText("正在下载 0%");
            registerReceiver();
            poll();
        } catch (Throwable e) {
            status.setText("下载失败，请稍后重试");
            action.setEnabled(true);
            action.setAlpha(1f);
        }
    }

    private void registerReceiver() {
        if (receiverOn) return;
        IntentFilter f = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(done, f, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(done, f);
        }
        receiverOn = true;
    }

    private void poll() {
        ui.postDelayed(() -> {
            if (downloadId < 0) return;
            DownloadManager.Query q = new DownloadManager.Query().setFilterById(downloadId);
            try (android.database.Cursor c = dm.query(q)) {
                if (c != null && c.moveToFirst()) {
                    int total = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    int now = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int statusCode = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (total > 0) {
                        int pct = Math.max(0, Math.min(100, now * 100 / total));
                        progress.setProgress(pct);
                        status.setText("正在下载 " + pct + "%");
                    }
                    if (statusCode == DownloadManager.STATUS_SUCCESSFUL) {
                        finishDownload();
                        return;
                    }
                    if (statusCode == DownloadManager.STATUS_FAILED) {
                        status.setText("下载失败，请检查网络后重试");
                        action.setEnabled(true);
                        action.setAlpha(1f);
                        return;
                    }
                }
            } catch (Throwable ignored) {}
            poll();
        }, 500);
    }

    private void finishDownload() {
        try {
            progress.setProgress(100);
            status.setText("下载完成，正在打开安装程序");
            Uri file = dm.getUriForDownloadedFile(downloadId);
            if (file == null) {
                file = Uri.parse("content://" + getPackageName() + ApkProvider.AUTHORITY_SUFFIX + "/" + FILE_NAME);
            }
            if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
                status.setText("请允许安装未知应用后返回继续");
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + getPackageName())));
                } catch (Throwable ignored) {}
                return;
            }
            // 安装前校验签名：下载到的 APK 必须与当前 App 同签名，否则拒绝（防 DNS 劫持/下载替换）
            if (!verifyDownloadedApk()) {
                status.setText("安装包校验失败，已阻止安装，请到官网重新下载");
                action.setEnabled(true);
                action.setAlpha(1f);
                return;
            }
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(file, "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            if (!force) finish();
        } catch (Throwable e) {
            status.setText("安装程序打开失败，请到下载目录手动安装");
        }
    }

    /** 安装前校验：下载的 APK 必须与当前 App 同签名。 */
    private boolean verifyDownloadedApk() {
        try {
            java.io.File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null) return false;
            java.io.File apk = new java.io.File(dir, FILE_NAME);
            if (!apk.isFile() || apk.length() < 1024L) return false;

            android.content.pm.PackageManager pm = getPackageManager();
            int flag = Build.VERSION.SDK_INT >= 28
                    ? android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                    : android.content.pm.PackageManager.GET_SIGNATURES;
            android.content.pm.PackageInfo info = pm.getPackageArchiveInfo(apk.getAbsolutePath(), flag);
            android.content.pm.PackageInfo self = pm.getPackageInfo(getPackageName(), flag);
            byte[] a = firstSigner(info);
            byte[] b = firstSigner(self);
            return a != null && b != null && java.security.MessageDigest.isEqual(a, b);
        } catch (Throwable e) {
            return false;
        }
    }

    private static byte[] firstSigner(android.content.pm.PackageInfo info) {
        if (info == null) return null;
        android.content.pm.Signature[] sigs;
        if (Build.VERSION.SDK_INT >= 28) {
            android.content.pm.SigningInfo si = info.signingInfo;
            if (si == null) return null;
            sigs = si.hasMultipleSigners() ? si.getApkContentsSigners() : si.getSigningCertificateHistory();
        } else {
            sigs = info.signatures;
        }
        if (sigs == null || sigs.length == 0) return null;
        return sigs[0].toByteArray();
    }

    @Override public void onBackPressed() {
        if (force) {
            Toast.makeText(this, "本次更新为强制更新", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }

    @Override protected void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        if (receiverOn) {
            try { unregisterReceiver(done); } catch (Throwable ignored) {}
            receiverOn = false;
        }
        super.onDestroy();
    }

    private TextView text(String s, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private int dp(float v) { return HsTheme.dp(this, v); }
}
