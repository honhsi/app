package com.hs.club;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VipActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private TextView balanceView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HsTheme.init(this);
        HsTheme.apply(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(HsTheme.bg);
        setContentView(root);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(16), dp(12), dp(16), dp(8));
        TextView back = text("‹", 28, HsTheme.a, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(32), dp(40)));
        bar.addView(text("VIP 中心", 18, HsTheme.text, true),
                new LinearLayout.LayoutParams(0, dp(40), 1));
        TextView rules = text("规则", 10, HsTheme.a, false);
        rules.setOnClickListener(v -> open("/vip"));
        bar.addView(rules);
        root.addView(bar);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), 0, dp(16), dp(14));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        ImageView banner = new ImageView(this);
        banner.setScaleType(ImageView.ScaleType.CENTER_CROP);
        banner.setBackgroundColor(HsTheme.bg2);
        body.addView(banner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(126)));
        pool.execute(() -> {
            Bitmap bm = Api.getBitmap(Api.base() + "/assets/banners/banner-2.jpg");
            if (bm != null) ui.post(() -> banner.setImageBitmap(bm));
        });

        LinearLayout level = new LinearLayout(this);
        level.setOrientation(LinearLayout.VERTICAL);
        level.setPadding(dp(16), dp(14), dp(16), dp(14));
        level.setBackground(HsTheme.round(HsTheme.panel, 16, this));
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.topMargin = dp(12);
        body.addView(level, llp);
        level.addView(text("VIP 3", 22, HsTheme.gold, true));
        balanceView = text("账户余额 ¥0.00", 10, HsTheme.muted, false);
        level.addView(balanceView);
        level.addView(text("再充值 ¥12,000 升级 VIP 4", 9, HsTheme.muted, false));
        View progress = new View(this);
        progress.setBackground(HsTheme.round(HsTheme.gold, 3, this));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(dp(210), dp(6));
        plp.topMargin = dp(10);
        level.addView(progress, plp);
        level.addView(text("当前进度 62%", 8, HsTheme.gold, true));

        body.addView(text("VIP 专属权益", 13, HsTheme.text, true),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));
        String[][] perks = {
                {"客", "专属客服", "7×24 优先接入"},
                {"闪", "极速提现", "优先审核通道"},
                {"礼", "生日礼金", "每年一次"},
                {"包", "周月红包", "按等级发放"}
        };
        for (int i = 0; i < perks.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 0, 0, dp(10));
            for (int j = i; j < Math.min(i + 2, perks.length); j++) {
                final String[] p = perks[j];
                LinearLayout item = new LinearLayout(this);
                item.setGravity(Gravity.CENTER_VERTICAL);
                item.setPadding(dp(12), dp(10), dp(12), dp(10));
                item.setBackground(HsTheme.round(HsTheme.panel, 14, this));
                TextView ic = text(p[0], 10, HsTheme.b, true);
                ic.setGravity(Gravity.CENTER);
                ic.setBackground(HsTheme.round(HsTheme.glow, 12, this));
                item.addView(ic, new LinearLayout.LayoutParams(dp(32), dp(32)));
                LinearLayout info = new LinearLayout(this);
                info.setOrientation(LinearLayout.VERTICAL);
                info.setPadding(dp(9), 0, 0, 0);
                info.addView(text(p[1], 9, HsTheme.text, true));
                info.addView(text(p[2], 7, HsTheme.muted, false));
                item.addView(info);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(56), 1);
                lp.rightMargin = j == i ? dp(10) : 0;
                row.addView(item, lp);
            }
            body.addView(row);
        }

        LinearLayout packet = new LinearLayout(this);
        packet.setGravity(Gravity.CENTER_VERTICAL);
        packet.setPadding(dp(14), dp(10), dp(14), dp(10));
        packet.setBackground(HsTheme.round(HsTheme.glow, 14, this));
        LinearLayout.LayoutParams pklp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(62));
        pklp.topMargin = dp(4);
        body.addView(packet, pklp);
        LinearLayout pinfo = new LinearLayout(this);
        pinfo.setOrientation(LinearLayout.VERTICAL);
        pinfo.addView(text("本周 VIP 红包", 10, HsTheme.text, true));
        pinfo.addView(text("可领取 ¥88.00 · 已完成打码", 8, HsTheme.muted, false));
        packet.addView(pinfo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView claim = text("立即领取", 8, Color.WHITE, true);
        claim.setGravity(Gravity.CENTER);
        claim.setBackground(HsTheme.round(HsTheme.gold, 13, this));
        claim.setOnClickListener(v -> open("/vip"));
        packet.addView(claim, new LinearLayout.LayoutParams(dp(82), dp(32)));

        HsNav.add(this, root, 4);
        load();
    }

    private void addBottomNav(LinearLayout root) {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(HsTheme.panel);
        nav.setPadding(dp(6), dp(6), dp(6), dp(8));
        String[] labels = {"首页", "活动", "钱包", "我的"};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView v = text(labels[i], 9, i == 3 ? HsTheme.b : HsTheme.muted, i == 3);
            v.setGravity(Gravity.CENTER);
            if (i == 3) v.setBackground(HsTheme.round(HsTheme.glow, 12, this));
            v.setOnClickListener(x -> {
                if (idx == 0) startActivity(new Intent(this, NativeHomeActivity.class));
                else if (idx == 1) startActivity(new Intent(this, ActivitiesActivity.class));
                else if (idx == 2) startActivity(new Intent(this, WalletActivity.class));
            });
            nav.addView(v, new LinearLayout.LayoutParams(0, dp(40), 1));
        }
        root.addView(nav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
    }

    private void load() {
        pool.execute(() -> {
            JSONObject s = Api.session();
            if (s == null) return;
            final double bal = s.optDouble("balance", 0);
            ui.post(() -> balanceView.setText("账户余额 ¥" + String.format("%.2f", bal)));
        });
    }

    private void open(String path) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(MainActivity.EXTRA_URL, Api.base() + path);
        startActivity(i);
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
