package com.hs.club;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.PictureDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.caverock.androidsvg.SVG;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NativeHomeActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private final LruCache<String, Bitmap> images = new LruCache<String, Bitmap>(24 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
    };
    private FrameLayout root;
    private LinearLayout categoryBar;
    private LinearLayout hotRow;
    private LinearLayout bannerRow;
    private ListView gameList;
    private TextView balanceView;
    private TextView userView;
    private final Map<String, List<Game>> catalogs = new LinkedHashMap<>();
    private String currentCategory = "";
    private GameAdapter adapter;
    private boolean loggedIn = false;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#f8f5ff"));
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#f8f5ff"));
        setContentView(root);
        showLoading("正在连接...");
        checkUpdate();
        pool.execute(() -> {
            JSONObject s = Api.session();
            final boolean login = s != null && s.optBoolean("login", false);
            final String name = s == null ? "" : s.optString("username", "");
            final double bal = s == null ? 0 : s.optDouble("balance", 0);
            ui.post(() -> {
                loggedIn = login;
                showHome(login ? name : "", login ? bal : 0);
            });
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (root != null) refreshSession();
    }

    private void showLoading(String text) {
        root.removeAllViews();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(Color.parseColor("#f8f5ff"));
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#7c6cff"));
        tv.setTextSize(14);
        ProgressBar pb = new ProgressBar(this);
        pb.getIndeterminateDrawable().setColorFilter(Color.parseColor("#7c6cff"), android.graphics.PorterDuff.Mode.SRC_IN);
        box.addView(pb, new LinearLayout.LayoutParams(dp(40), dp(40)));
        box.addView(tv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(box, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void checkUpdate() {
        pool.execute(() -> {
            try {
                JSONObject json = Api.getJson("/app_config.php");
                final int remote = json.optInt("version_code", 0);
                if (remote <= BuildConfig.VERSION_CODE) return;
                final String name = json.optString("version_name", "");
                final String changelog = json.optString("changelog", "");
                String download = json.optString("download_url", "");
                if (download.isEmpty()) download = json.optString("official_url", BuildConfig.OFFICIAL_URL);
                final String dl = download;
                final boolean force = json.optInt("force_update", 0) == 1;
                ui.post(() -> {
                    android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this)
                            .setTitle("发现新版本 " + name)
                            .setMessage(changelog.isEmpty() ? "请下载最新版本" : changelog)
                            .setPositiveButton("去下载", (d, w) -> {
                                if (!dl.isEmpty()) {
                                    try { startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(dl))); } catch (Exception ignored) {}
                                }
                            });
                    if (!force) b.setNegativeButton("以后再说", null);
                    b.setCancelable(!force);
                    b.show();
                });
            } catch (Exception ignored) {}
        });
    }

    private void showLogin() {
        root.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(26), dp(52), dp(26), dp(32));
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setBackgroundColor(Color.parseColor("#12091c"));
        scroll.addView(box);

        TextView logo = text("HS", 44, "#e6c27a", true);
        logo.setGravity(Gravity.CENTER);
        box.addView(logo);
        TextView title = text(BuildConfig.APP_NAME, 24, "#ffffff", true);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(8), 0, dp(4));
        box.addView(title);
        TextView sub = text("登录后进入游戏大厅", 13, "#9b8fb5", false);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 0, 0, dp(28));
        box.addView(sub);

        EditText user = input("用户名 / 手机号 / 邮箱", false);
        box.addView(user);
        EditText pass = input("密码", true);
        box.addView(pass);

        ImageView captcha = new ImageView(this);
        captcha.setBackgroundColor(Color.parseColor("#1d1329"));
        captcha.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72));
        clp.topMargin = dp(10);
        box.addView(captcha, clp);
        EditText code = input("验证码", false);
        box.addView(code);

        TextView err = text("", 12, "#ff8a8a", false);
        err.setPadding(0, dp(8), 0, 0);
        box.addView(err);

        Button login = button("登录", "#7c6cff", "#ffffff");
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        blp.topMargin = dp(20);
        box.addView(login, blp);
        Button guest = button("先看看", "#241733", "#cbb8ff");
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        glp.topMargin = dp(10);
        box.addView(guest, glp);

        final Runnable refresh = () -> pool.execute(() -> {
            Bitmap bm = null;
            try {
                bm = renderSvg(Api.getBytes("/captcha.php?t=" + System.currentTimeMillis()));
            } catch (Exception ignored) {}
            final Bitmap out = bm;
            ui.post(() -> { if (out != null) captcha.setImageBitmap(out); else captcha.setImageDrawable(null); });
        });
        refresh.run();
        captcha.setOnClickListener(v -> refresh.run());

        login.setOnClickListener(v -> {
            String u = user.getText().toString().trim();
            String p = pass.getText().toString();
            String c = code.getText().toString().trim();
            if (u.isEmpty() || p.isEmpty()) { err.setText("请输入账号和密码"); return; }
            login.setEnabled(false);
            login.setText("登录中...");
            pool.execute(() -> {
                try {
                    Map<String, String> f = new HashMap<>();
                    f.put("action", "login");
                    f.put("username", u);
                    f.put("password", p);
                    f.put("captcha", c);
                    JSONObject r = Api.post("/login.php?ajax=1", f);
                    if (r.optBoolean("ok", false)) {
                        JSONObject s = Api.session();
                        final String name = s == null ? u : s.optString("username", u);
                        final double bal = s == null ? 0 : s.optDouble("balance", 0);
                        ui.post(() -> showHome(name, bal));
                    } else {
                        final String msg = r.optString("message", "登录失败");
                        ui.post(() -> {
                            err.setText(msg);
                            login.setEnabled(true);
                            login.setText("登录");
                            refresh.run();
                        });
                    }
                } catch (Exception e) {
                    ui.post(() -> {
                        err.setText("网络异常，请稍后重试");
                        login.setEnabled(true);
                        login.setText("登录");
                    });
                }
            });
        });
        guest.setOnClickListener(v -> showHome("", 0));
        root.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private Bitmap renderSvg(byte[] bytes) {
        try {
            SVG svg = SVG.getFromInputStream(new java.io.ByteArrayInputStream(bytes));
            Picture picture = svg.renderToPicture();
            if (picture == null) return null;
            int w = Math.max(1, (int)svg.getDocumentWidth() * 2);
            int h = Math.max(1, (int)svg.getDocumentHeight() * 2);
            Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bm);
            canvas.drawColor(Color.parseColor("#14110b"));
            PictureDrawable pd = new PictureDrawable(picture);
            pd.setBounds(0, 0, w, h);
            pd.draw(canvas);
            return bm;
        } catch (Throwable e) {
            return null;
        }
    }

    private void showHome(String username, double balance) {
        root.removeAllViews();
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.parseColor("#f8f5ff"));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(16), dp(18), dp(16), dp(12));
        GradientDrawable hb = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#ffffff"), Color.parseColor("#f3edff")});
        header.setBackground(hb);
        header.setElevation(dp(2));
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView app = text(BuildConfig.APP_NAME, 18, "#2b2042", true);
        top.addView(app, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        userView = text(username.isEmpty() ? "未登录" : username, 12, "#7c6cff", false);
        userView.setPadding(dp(8), 0, 0, 0);
        userView.setOnClickListener(v -> {
            if (!loggedIn) openLogin();
            else startActivity(new Intent(this, ProfileActivity.class));
        });
        top.addView(userView);
        header.addView(top);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, 0);
        balanceView = text("¥ " + String.format("%.2f", balance), 26, "#2b2042", true);
        row.addView(balanceView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button dep = smallButton("充值", "#7c6cff", "#ffffff");
        Button wd = smallButton("提现", "#e6c27a", "#241733");
        Button wallet = smallButton("钱包", "#f1ecff", "#7c6cff");
        row.addView(wallet);
        row.addView(dep);
        row.addView(wd);
        header.addView(row);
        page.addView(header);
        page.addView(buildBanners());

        categoryBar = new LinearLayout(this);
        categoryBar.setOrientation(LinearLayout.HORIZONTAL);
        categoryBar.setPadding(dp(10), dp(6), dp(10), dp(4));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(categoryBar);
        page.addView(hs);

        TextView hotTitle = text("热门推荐", 15, "#2b2042", true);
        hotTitle.setPadding(dp(14), dp(8), dp(14), dp(2));
        page.addView(hotTitle);
        hotRow = new LinearLayout(this);
        hotRow.setOrientation(LinearLayout.HORIZONTAL);
        hotRow.setPadding(dp(10), dp(4), dp(10), dp(8));
        HorizontalScrollView hotScroll = new HorizontalScrollView(this);
        hotScroll.setHorizontalScrollBarEnabled(false);
        hotScroll.addView(hotRow);
        page.addView(hotScroll);

        gameList = new ListView(this);
        gameList.setDivider(new ColorDrawable(Color.parseColor("#eee8ff")));
        gameList.setDividerHeight(dp(1));
        gameList.setBackgroundColor(Color.parseColor("#f8f5ff"));
        page.addView(gameList, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        adapter = new GameAdapter();
        gameList.setAdapter(adapter);
        addBottomNav(page);
        root.addView(page, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        dep.setOnClickListener(v -> openWeb("/recharge"));
        wd.setOnClickListener(v -> openWeb("/withdraw"));
        wallet.setOnClickListener(v -> startActivity(new Intent(this, WalletActivity.class)));
        loadCatalog();
        refreshSession();
    }

    private android.view.View buildBanners() {
        bannerRow = new LinearLayout(this);
        bannerRow.setOrientation(LinearLayout.HORIZONTAL);
        bannerRow.setPadding(dp(12), dp(10), dp(12), dp(6));
        final String[] imgs = {
                "/assets/banners/banner-1.jpg",
                "/assets/banners/banner-2.jpg",
                "/assets/banners/banner-3.jpg",
                "/assets/banners/banner-4.jpg"
        };
        final String[] links = {"/deposit", "/vip", "/tasks", "/agent"};
        for (int i = 0; i < imgs.length; i++) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackgroundColor(Color.parseColor("#241733"));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#241733"));
            bg.setCornerRadius(dp(14));
            iv.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(300), dp(132));
            lp.rightMargin = dp(10);
            bannerRow.addView(iv, lp);
            final String path = imgs[i];
            final String link = links[i];
            iv.setTag(path);
            pool.execute(() -> {
                Bitmap bm = Api.getBitmap(Api.base() + path);
                if (bm != null) {
                    images.put(path, bm);
                    ui.post(() -> {
                        if (path.equals(iv.getTag())) iv.setImageBitmap(bm);
                    });
                }
            });
            iv.setOnClickListener(v -> openWeb(link));
        }
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.addView(bannerRow);
        return scroll;
    }

    private void addBottomNav(LinearLayout page) {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, dp(4), 0, dp(6));
        nav.setBackgroundColor(Color.WHITE);
        nav.setElevation(dp(8));
        String[][] items = {
                {"⌂", "首页"}, {"🎁", "活动"}, {"¥", "充值"}, {"⇩", "提现"}, {"☺", "我的"}
        };
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            TextView icon = text(items[i][0], 18, i == 0 ? "#7c6cff" : "#8a7ba2", false);
            icon.setGravity(Gravity.CENTER);
            TextView label = text(items[i][1], 10, i == 0 ? "#7c6cff" : "#8a7ba2", false);
            label.setGravity(Gravity.CENTER);
            cell.addView(icon);
            cell.addView(label);
            cell.setOnClickListener(v -> {
                if (idx == 0) return;
                if (idx == 1) openWeb("/promotions");
                else if (idx == 2) openWeb("/recharge");
                else if (idx == 3) openWeb("/withdraw");
                else startActivity(new Intent(this, ProfileActivity.class));
            });
            nav.addView(cell, new LinearLayout.LayoutParams(0, dp(52), 1));
        }
        page.addView(nav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)));
    }

    private void buildHotRow() {
        if (hotRow == null) return;
        hotRow.removeAllViews();
        List<Game> hot = catalogs.get("热门");
        if (hot == null || hot.isEmpty()) return;
        int max = Math.min(10, hot.size());
        for (int i = 0; i < max; i++) {
            final Game g = hot.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER_HORIZONTAL);
            card.setPadding(dp(4), dp(4), dp(4), dp(4));
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#241733"));
            bg.setCornerRadius(dp(12));
            iv.setBackground(bg);
            card.addView(iv, new LinearLayout.LayoutParams(dp(92), dp(92)));
            TextView name = text(g.title, 11, "#2b2042", false);
            name.setMaxLines(1);
            name.setPadding(0, dp(4), 0, 0);
            card.addView(name);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(100), ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(6);
            hotRow.addView(card, lp);
            final String path = g.img;
            iv.setTag(path);
            Bitmap cached = images.get(path);
            if (cached != null) iv.setImageBitmap(cached);
            else pool.execute(() -> {
                Bitmap bm = Api.getBitmap(path);
                if (bm != null) {
                    images.put(path, bm);
                    ui.post(() -> { if (path.equals(iv.getTag())) iv.setImageBitmap(bm); });
                }
            });
            card.setOnClickListener(v -> play(g));
        }
    }

    private void refreshSession() {
        pool.execute(() -> {
            JSONObject s = Api.session();
            if (s == null) return;
            final boolean login = s.optBoolean("login", false);
            final double bal = s.optDouble("balance", 0);
            final String name = s.optString("username", "");
            ui.post(() -> {
                loggedIn = login;
                if (balanceView != null) balanceView.setText("¥ " + String.format("%.2f", bal));
                if (userView != null) userView.setText(login && !name.isEmpty() ? name : "未登录");
            });
        });
    }

    private void openLogin() {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(MainActivity.EXTRA_URL, Api.base() + "/login.php");
        startActivity(i);
    }

    private void loadCatalog() {
        pool.execute(() -> {
            try {
                JSONObject json = Api.getJson("/?catalog=1");
                final Map<String, List<Game>> data = new LinkedHashMap<>();
                Iterator<String> cats = json.keys();
                while (cats.hasNext()) {
                    String cat = cats.next();
                    JSONObject vendors = json.optJSONObject(cat);
                    if (vendors == null) continue;
                    List<Game> games = new ArrayList<>();
                    Iterator<String> vs = vendors.keys();
                    while (vs.hasNext()) {
                        String vendor = vs.next();
                        JSONArray arr = vendors.optJSONArray(vendor);
                        if (arr == null) continue;
                        for (int i = 0; i < arr.length(); i++) {
                            JSONArray g = arr.optJSONArray(i);
                            if (g == null || g.length() < 3) continue;
                            Game game = new Game();
                            game.title = g.optString(0, "");
                            game.img = g.optString(1, "");
                            game.code = g.optString(2, "");
                            game.type = g.optInt(3, 3);
                            game.vendor = vendor;
                            games.add(game);
                        }
                    }
                    if (!games.isEmpty()) data.put(cat, games);
                }
                List<Game> hot = new ArrayList<>();
                Set<String> seen = new HashSet<>();
                for (List<Game> games : data.values()) {
                    for (Game g : games) {
                        String key = g.vendor + "|" + g.code;
                        if (seen.add(key)) hot.add(g);
                        if (hot.size() >= 30) break;
                    }
                    if (hot.size() >= 30) break;
                }
                final Map<String, List<Game>> ordered = new LinkedHashMap<>();
                if (!hot.isEmpty()) ordered.put("热门", hot);
                for (Map.Entry<String, List<Game>> e : data.entrySet()) {
                    if (!"热门".equals(e.getKey())) ordered.put(e.getKey(), e.getValue());
                }
                ui.post(() -> {
                    catalogs.clear();
                    catalogs.putAll(ordered);
                    buildCategories();
                });
            } catch (Exception e) {
                ui.post(() -> Toast.makeText(this, "加载游戏失败，请下拉重试", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void buildCategories() {
        if (categoryBar == null) return;
        categoryBar.removeAllViews();
        for (String cat : catalogs.keySet()) {
            Button b = smallButton(cat, cat.equals("热门") ? "#7c6cff" : "#ffffff", cat.equals("热门") ? "#ffffff" : "#4a3b63");
            b.setOnClickListener(v -> {
                for (int i = 0; i < categoryBar.getChildCount(); i++) {
                    View child = categoryBar.getChildAt(i);
                    if (child instanceof Button) {
                        ((Button) child).setTextColor(Color.parseColor("#4a3b63"));
                        ((Button) child).setBackground(roundBg("#ffffff", 18));
                    }
                }
                b.setTextColor(Color.WHITE);
                b.setBackground(roundBg("#7c6cff", 18));
                showCategory(cat);
            });
            categoryBar.addView(b);
        }
        String first = catalogs.containsKey("热门") ? "热门" : (catalogs.isEmpty() ? "" : catalogs.keySet().iterator().next());
        if (!first.isEmpty()) showCategory(first);
        buildHotRow();
    }

    private void showCategory(String cat) {
        currentCategory = cat;
        List<Game> list = catalogs.get(cat);
        adapter.setData(list == null ? new ArrayList<>() : list);
    }

    private void openWeb(String path) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(MainActivity.EXTRA_URL, Api.base() + path);
        startActivity(i);
    }

    private void play(Game g) {
        if (!loggedIn) { openLogin(); return; }
        Toast.makeText(this, "正在进入 " + g.title, Toast.LENGTH_SHORT).show();
        pool.execute(() -> {
            JSONObject r = Api.playGame(g.vendor, g.code, g.type);
            final String url = r == null ? "" : (r.optString("Url", r.optString("url", "")));
            if (url.isEmpty()) {
                final String msg = r == null ? "启动失败" : r.optString("Message", "启动失败");
                ui.post(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
                return;
            }
            ui.post(() -> {
                Intent i = new Intent(this, MainActivity.class);
                i.putExtra(MainActivity.EXTRA_URL, url);
                i.putExtra(MainActivity.EXTRA_GAME, true);
                i.putExtra(MainActivity.EXTRA_VENDOR, g.vendor);
                startActivity(i);
            });
        });
    }

    private class GameAdapter extends BaseAdapter {
        private final List<Game> data = new ArrayList<>();

        void setData(List<Game> list) {
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }

        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int position) { return data.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row = new LinearLayout(NativeHomeActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(8), dp(14), dp(8));
            row.setBackgroundColor(Color.WHITE);
            ImageView img = new ImageView(NativeHomeActivity.this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setBackgroundColor(Color.parseColor("#241733"));
            row.addView(img, new LinearLayout.LayoutParams(dp(68), dp(68)));
            LinearLayout texts = new LinearLayout(NativeHomeActivity.this);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(dp(12), 0, 0, 0);
            TextView t = text(data.get(position).title, 14, "#2b2042", true);
            TextView s = text(data.get(position).vendor + "  ·  点击进入", 11, "#8a7ba2", false);
            texts.addView(t);
            texts.addView(s);
            row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            final Game g = data.get(position);
            final String url = g.img;
            img.setTag(url);
            Bitmap bm = images.get(url);
            if (bm != null) img.setImageBitmap(bm);
            else pool.execute(() -> {
                Bitmap b = Api.getBitmap(url);
                if (b != null) {
                    images.put(url, b);
                    ui.post(() -> {
                        if (url.equals(img.getTag())) img.setImageBitmap(b);
                    });
                }
            });
            row.setOnClickListener(v -> play(g));
            return row;
        }
    }

    private static class Game {
        String title, img, code, vendor;
        int type;
    }

    private EditText input(String hint, boolean password) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.parseColor("#7b6d93"));
        e.setTextColor(Color.WHITE);
        e.setTextSize(14);
        e.setSingleLine(true);
        e.setPadding(dp(14), 0, dp(14), 0);
        e.setBackground(roundBg("#241733", 12));
        if (password) e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        lp.topMargin = dp(10);
        e.setLayoutParams(lp);
        return e;
    }

    private Button button(String label, String bg, String fg) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setTextColor(Color.parseColor(fg));
        b.setAllCaps(false);
        b.setBackground(roundBg(bg, 24));
        return b;
    }

    private Button smallButton(String label, String bg, String fg) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12);
        b.setTextColor(Color.parseColor(fg));
        b.setAllCaps(false);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(12), 0, dp(12), 0);
        b.setBackground(roundBg(bg, 18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
        lp.rightMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    private TextView text(String value, int sp, String color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(Color.parseColor(color));
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
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
