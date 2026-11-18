package com.hs.club;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private TextView nameView;
    private TextView idView;
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
        bar.setPadding(dp(18), dp(12), dp(18), dp(8));
        bar.addView(text("个人中心", 18, HsTheme.text, true),
                new LinearLayout.LayoutParams(0, dp(40), 1));
        TextView settings = text("设置", 11, HsTheme.muted, false);
        settings.setOnClickListener(v -> open("/profile"));
        bar.addView(settings);
        root.addView(bar);

        // 用户卡
        LinearLayout user = new LinearLayout(this);
        user.setGravity(Gravity.CENTER_VERTICAL);
        user.setPadding(dp(16), dp(16), dp(16), dp(16));
        user.setBackground(HsTheme.round(HsTheme.panel, 18, this));
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(104));
        ulp.setMargins(dp(16), dp(4), dp(16), 0);
        root.addView(user, ulp);
        TextView avatar = text("龙", 21, HsTheme.b, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(HsTheme.round(HsTheme.glow, 30, this));
        user.addView(avatar, new LinearLayout.LayoutParams(dp(60), dp(60)));
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(14), 0, 0, 0);
        nameView = text("未登录", 16, HsTheme.text, true);
        info.addView(nameView);
        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView vip = text("VIP 3", 8, HsTheme.gold, true);
        vip.setGravity(Gravity.CENTER);
        vip.setBackground(HsTheme.round(HsTheme.glow, 9, this));
        badgeRow.addView(vip, new LinearLayout.LayoutParams(dp(46), dp(20)));
        idView = text("ID --", 8, HsTheme.muted, false);
        idView.setPadding(dp(8), 0, 0, 0);
        badgeRow.addView(idView);
        info.addView(badgeRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(24)));
        balanceView = text("¥ 0.00", 13, HsTheme.a, true);
        info.addView(balanceView);
        user.addView(info, new LinearLayout.LayoutParams(0, dp(74), 1));
        TextView arrow = text("›", 20, HsTheme.muted, false);
        user.addView(arrow);

        // VIP 卡
        LinearLayout vipCard = new LinearLayout(this);
        vipCard.setOrientation(LinearLayout.VERTICAL);
        vipCard.setPadding(dp(16), dp(14), dp(16), dp(12));
        vipCard.setBackground(HsTheme.round(HsTheme.glow, 16, this));
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(112));
        vlp.setMargins(dp(16), dp(14), dp(16), 0);
        root.addView(vipCard, vlp);
        vipCard.addView(text("VIP 3 专属权益", 13, HsTheme.gold, true));
        vipCard.addView(text("再充值 ¥12,000 升级 VIP 4", 9, HsTheme.muted, false));
        View progress = new View(this);
        progress.setBackground(HsTheme.round(HsTheme.gold, 3, this));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(dp(210), dp(6));
        plp.topMargin = dp(12);
        vipCard.addView(progress, plp);
        vipCard.addView(text("当前进度 62%", 8, HsTheme.gold, true));
        vipCard.setOnClickListener(v -> startActivity(new Intent(this, VipActivity.class)));

        // 菜单
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(6), dp(4), dp(6), dp(4));
        menu.setBackground(HsTheme.round(HsTheme.panel, 18, this));
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(216));
        mlp.setMargins(dp(16), dp(14), dp(16), 0);
        root.addView(menu, mlp);
        addItem(menu, "设", "登录设备", "查看当前在线设备", "/devices");
        addItem(menu, "记", "登录记录", "最近登录 IP 与设备", "/login_log");
        addItem(menu, "安", "安全设置", "密码 / 提现密码", "/security");
        addItem(menu, "客", "在线客服", "7×24 小时在线", "/support");

        LinearLayout sync = new LinearLayout(this);
        sync.setOrientation(LinearLayout.VERTICAL);
        sync.setPadding(dp(14), dp(10), dp(14), dp(10));
        sync.setBackground(HsTheme.round(HsTheme.glow, 14, this));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        slp.setMargins(dp(16), dp(14), dp(16), 0);
        root.addView(sync, slp);
        sync.addView(text("主题同步", 10, HsTheme.text, true));
        sync.addView(text("颜色 / 卡片 / 按钮 / 底部导航统一由 HsTheme 控制", 7, HsTheme.muted, false));

        HsNav.add(this, root, 4);
        load();
    }

    private void addItem(LinearLayout parent, String icon, String title, String desc, String path) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), 0, dp(8), 0);
        TextView ic = text(icon, 9, HsTheme.b, true);
        ic.setGravity(Gravity.CENTER);
        ic.setBackground(HsTheme.round(HsTheme.glow, 12, this));
        row.addView(ic, new LinearLayout.LayoutParams(dp(32), dp(32)));
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(10), 0, 0, 0);
        info.addView(text(title, 10, HsTheme.text, true));
        info.addView(text(desc, 7, HsTheme.muted, false));
        row.addView(info, new LinearLayout.LayoutParams(0, dp(44), 1));
        row.addView(text("›", 15, HsTheme.muted, false));
        row.setOnClickListener(v -> open(path));
        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
    }

    private void addBottomNav(LinearLayout root) {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
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
            nav.addView(v, new LinearLayout.LayoutParams(0, dp(42), 1));
        }
        root.addView(nav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
    }

    private void load() {
        pool.execute(() -> {
            JSONObject s = Api.session();
            if (s == null) return;
            final String name = s.optString("username", "");
            final double bal = s.optDouble("balance", 0);
            ui.post(() -> {
                if (!name.isEmpty()) nameView.setText(name);
                idView.setText(name.isEmpty() ? "未登录" : ("账号 " + name));
                balanceView.setText("¥ " + String.format("%.2f", bal));
            });
        });
    }

    private void open(String path) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(MainActivity.EXTRA_URL, Api.base() + path);
        startActivity(i);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private int dp(float v) { return HsTheme.dp(this, v); }
}
