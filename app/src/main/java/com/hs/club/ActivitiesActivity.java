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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivitiesActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newFixedThreadPool(3);

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
        bar.addView(text("活动中心", 18, HsTheme.text, true),
                new LinearLayout.LayoutParams(0, dp(40), 1));
        TextView rules = text("规则", 10, HsTheme.a, false);
        rules.setOnClickListener(v -> open("/promotions"));
        bar.addView(rules);
        root.addView(bar);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(dp(16), dp(2), dp(16), dp(8));
        String[] labels = {"全部", "首存", "VIP", "任务"};
        for (int i = 0; i < labels.length; i++) {
            TextView t = text(labels[i], 9, i == 0 ? Color.WHITE : HsTheme.muted, i == 0);
            t.setGravity(Gravity.CENTER);
            t.setBackground(i == 0 ? HsTheme.round(HsTheme.b, 13, this)
                    : HsTheme.round(HsTheme.panel, 13, this));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(60), dp(28));
            lp.rightMargin = dp(8);
            tabs.addView(t, lp);
        }
        root.addView(tabs);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), 0, dp(16), dp(14));
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        addBanner(list, "banners_banner-1.jpg", "首充加码 10%", "新会员专享 · 最高送 ¥888", "立即充值", "/recharge");
        addBanner(list, "banners_banner-2.jpg", "VIP 尊享 888", "升级即领 · 专属客服极速提现", "查看权益", "/vip");
        addBanner(list, "banners_banner-3.jpg", "每日任务 彩金不断", "签到 / 投注 / 邀请三重奖励", "立即做任务", "/tasks");
        addBanner(list, "banners_banner-4.jpg", "成为代理 佣金最高 55%", "实时结算 · 团队管理 · 专属后台", "了解代理", "/agent");

        HsNav.add(this, root, 1);
    }

    private void addBanner(LinearLayout parent, String file, String title, String desc, String btn, String path) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(12));
        card.setBackground(HsTheme.round(HsTheme.panel, 16, this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        parent.addView(card, lp);

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(HsTheme.bg2);
        card.addView(image, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(126)));
        pool.execute(() -> {
            Bitmap bm = Api.getBitmap(Api.base() + "/assets/banners/" + file);
            if (bm != null) ui.post(() -> image.setImageBitmap(bm));
        });

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(0, dp(8), 0, 0);
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.addView(text(title, 12, HsTheme.text, true));
        info.addView(text(desc, 8, HsTheme.muted, false));
        bottom.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView go = text(btn, 8, Color.WHITE, true);
        go.setGravity(Gravity.CENTER);
        go.setBackground(HsTheme.gradient(13, this));
        go.setOnClickListener(v -> {
            if ("/vip".equals(path)) startActivity(new Intent(this, VipActivity.class));
            else open(path);
        });
        bottom.addView(go, new LinearLayout.LayoutParams(dp(92), dp(30)));
        card.addView(bottom);
        card.setOnClickListener(v -> {
            if ("/vip".equals(path)) startActivity(new Intent(this, VipActivity.class));
            else open(path);
        });
    }

    private void addBottomNav(LinearLayout root) {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(HsTheme.panel);
        nav.setPadding(dp(6), dp(6), dp(6), dp(8));
        String[] labels = {"首页", "活动", "钱包", "我的"};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView v = text(labels[i], 9, i == 1 ? HsTheme.b : HsTheme.muted, i == 1);
            v.setGravity(Gravity.CENTER);
            if (i == 1) v.setBackground(HsTheme.round(HsTheme.glow, 12, this));
            v.setOnClickListener(x -> {
                if (idx == 0) startActivity(new Intent(this, NativeHomeActivity.class));
                else if (idx == 2) startActivity(new Intent(this, WalletActivity.class));
                else if (idx == 3) startActivity(new Intent(this, ProfileActivity.class));
            });
            nav.addView(v, new LinearLayout.LayoutParams(0, dp(40), 1));
        }
        root.addView(nav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
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
