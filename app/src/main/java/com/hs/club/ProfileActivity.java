package com.hs.club;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private TextView nameView;
    private TextView balanceView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#12091c"));
        getWindow().setNavigationBarColor(Color.parseColor("#12091c"));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#f8f5ff"));
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER_HORIZONTAL);
        header.setPadding(dp(18), dp(34), dp(18), dp(22));
        GradientDrawable hg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#241733"), Color.parseColor("#4a2f75")});
        header.setBackground(hg);
        TextView avatar = new TextView(this);
        avatar.setText("HS");
        avatar.setTextColor(Color.parseColor("#e6c27a"));
        avatar.setTextSize(30);
        avatar.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        avatar.setGravity(Gravity.CENTER);
        GradientDrawable ab = new GradientDrawable();
        ab.setColor(Color.parseColor("#3b2457"));
        ab.setCornerRadius(dp(36));
        avatar.setBackground(ab);
        header.addView(avatar, new LinearLayout.LayoutParams(dp(72), dp(72)));
        nameView = new TextView(this);
        nameView.setText("未登录");
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(18);
        nameView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nameView.setPadding(0, dp(12), 0, dp(2));
        header.addView(nameView);
        balanceView = new TextView(this);
        balanceView.setText("¥ 0.00");
        balanceView.setTextColor(Color.parseColor("#e6c27a"));
        balanceView.setTextSize(22);
        balanceView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(balanceView);
        root.addView(header);

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(16), dp(16), dp(16), 0);
        addItem(menu, "我的钱包", () -> startActivity(new Intent(this, WalletActivity.class)));
        addItem(menu, "充值", () -> open("/recharge"));
        addItem(menu, "提现", () -> open("/withdraw"));
        addItem(menu, "活动中心", () -> open("/promotions"));
        addItem(menu, "在线客服", () -> open("/support"));
        addItem(menu, "退出登录", this::logout);
        root.addView(menu);
        load();
    }

    private void addItem(LinearLayout parent, String label, Runnable action) {
        TextView row = new TextView(this);
        row.setText(label);
        row.setTextColor(Color.parseColor("#2b2042"));
        row.setTextSize(15);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), 0, dp(18), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(12));
        row.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        lp.bottomMargin = dp(10);
        row.setLayoutParams(lp);
        row.setOnClickListener(v -> action.run());
        parent.addView(row);
    }

    private void load() {
        pool.execute(() -> {
            JSONObject s = Api.session();
            if (s == null) return;
            final String name = s.optString("username", "");
            final double bal = s.optDouble("balance", 0);
            ui.post(() -> {
                if (!name.isEmpty()) nameView.setText(name);
                balanceView.setText("¥ " + String.format("%.2f", bal));
            });
        });
    }

    private void open(String path) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(MainActivity.EXTRA_URL, Api.base() + path);
        startActivity(i);
    }

    private void logout() {
        pool.execute(() -> {
            Api.logout();
            ui.post(() -> {
                Intent i = new Intent(this, NativeHomeActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            });
        });
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
