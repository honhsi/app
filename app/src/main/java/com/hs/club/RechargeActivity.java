package com.hs.club;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RechargeActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private EditText amountInput;
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
        bar.addView(text("充值", 18, HsTheme.text, true),
                new LinearLayout.LayoutParams(0, dp(40), 1));
        bar.addView(text("记录", 10, HsTheme.a, false));
        root.addView(bar);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), 0, dp(16), dp(12));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(HsTheme.round(HsTheme.panel, 16, this));
        body.addView(card);
        card.addView(text("充值金额", 11, HsTheme.text, true));
        amountInput = new EditText(this);
        amountInput.setHint("请输入充值金额");
        amountInput.setHintTextColor(HsTheme.muted);
        amountInput.setTextColor(HsTheme.text);
        amountInput.setTextSize(25);
        amountInput.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountInput.setBackground(null);
        amountInput.setPadding(0, dp(6), 0, dp(4));
        card.addView(amountInput);
        balanceView = text("当前余额 ¥0.00", 9, HsTheme.muted, false);
        card.addView(balanceView);

        LinearLayout quick = new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        quick.setPadding(0, dp(12), 0, 0);
        String[] amounts = {"100", "300", "500", "1000"};
        for (String a : amounts) {
            TextView chip = text("¥ " + a, 10, HsTheme.b, true);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(HsTheme.roundStroke(HsTheme.panel, HsTheme.line, 12, this));
            chip.setOnClickListener(v -> amountInput.setText(a));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(34), 1);
            lp.rightMargin = dp(8);
            quick.addView(chip, lp);
        }
        card.addView(quick);

        body.addView(text("支付方式", 12, HsTheme.text, true),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));
        String[][] methods = {
                {"支", "支付宝", "推荐", "76,143,255"},
                {"微", "微信支付", "即时到账", "31,157,99"},
                {"银", "银行卡", "转账", "217,138,34"},
                {"钱", "USDT", "数字货币", "124,108,255"}
        };
        for (String[] m : methods) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(8), dp(12), dp(8));
            row.setBackground(HsTheme.round(HsTheme.panel, 14, this));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
            lp.bottomMargin = dp(8);
            body.addView(row, lp);
            int rgb = Color.rgb(
                    Integer.parseInt(m[3].split(",")[0]),
                    Integer.parseInt(m[3].split(",")[1]),
                    Integer.parseInt(m[3].split(",")[2]));
            TextView ic = text(m[0], 11, rgb, true);
            ic.setGravity(Gravity.CENTER);
            ic.setBackground(HsTheme.round(HsTheme.glow, 12, this));
            row.addView(ic, new LinearLayout.LayoutParams(dp(34), dp(34)));
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(dp(10), 0, 0, 0);
            info.addView(text(m[1], 10, HsTheme.text, true));
            info.addView(text(m[2], 7, HsTheme.muted, false));
            row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            row.addView(text("›", 16, HsTheme.muted, false));
        }

        TextView tip = text("下一步选择具体支付通道并完成支付。金额、通道限制与到账规则以网页版为准。", 8, HsTheme.muted, false);
        tip.setLineSpacing(0, 1.4f);
        tip.setPadding(0, dp(2), 0, dp(10));
        body.addView(tip);

        TextView submit = text("下一步，选择支付方式", 13, Color.WHITE, true);
        submit.setGravity(Gravity.CENTER);
        submit.setBackground(HsTheme.gradient(14, this));
        submit.setOnClickListener(v -> next());
        body.addView(submit, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));

        HsNav.add(this, root, 2);
        load();
    }

    private void next() {
        String v = amountInput.getText().toString().trim();
        double amount = 0;
        try { amount = Double.parseDouble(v); } catch (Throwable ignored) {}
        if (amount <= 0) {
            amountInput.setError("请输入充值金额");
            return;
        }
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(MainActivity.EXTRA_URL, Api.base() + "/recharge?amount=" + String.format("%.2f", amount));
        startActivity(i);
    }

    private void load() {
        pool.execute(() -> {
            JSONObject s = Api.session();
            if (s == null) return;
            final double bal = s.optDouble("balance", 0);
            ui.post(() -> balanceView.setText("当前余额 ¥" + String.format("%.2f", bal)));
        });
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
