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
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WithdrawActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private EditText amountInput;
    private TextView balanceView;
    private double balance = 0;

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
        bar.addView(text("提现", 18, HsTheme.text, true),
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
        card.addView(text("可提现余额", 10, HsTheme.muted, false));
        balanceView = text("¥ 0.00", 27, HsTheme.text, true);
        balanceView.setPadding(0, dp(2), 0, dp(2));
        card.addView(balanceView);
        card.addView(text("待打码金额与提现限制以网页版为准", 8, HsTheme.muted, false));

        body.addView(text("提现金额", 12, HsTheme.text, true),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));
        LinearLayout amountBox = new LinearLayout(this);
        amountBox.setGravity(Gravity.CENTER_VERTICAL);
        amountBox.setPadding(dp(14), 0, dp(14), 0);
        amountBox.setBackground(HsTheme.roundStroke(HsTheme.panel, HsTheme.line, 14, this));
        amountInput = new EditText(this);
        amountInput.setHint("请输入提现金额");
        amountInput.setHintTextColor(HsTheme.muted);
        amountInput.setTextColor(HsTheme.text);
        amountInput.setTextSize(24);
        amountInput.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountInput.setBackground(null);
        amountBox.addView(amountInput, new LinearLayout.LayoutParams(0, dp(58), 1));
        TextView all = text("全部", 9, HsTheme.b, true);
        all.setOnClickListener(v -> {
            if (balance > 0) amountInput.setText(String.format("%.2f", balance));
        });
        amountBox.addView(all);
        body.addView(amountBox);

        LinearLayout quick = new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        quick.setPadding(0, dp(10), 0, dp(8));
        String[] amounts = {"100", "500", "1000", "2000"};
        for (String a : amounts) {
            TextView chip = text("¥ " + a, 9, HsTheme.b, true);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(HsTheme.roundStroke(HsTheme.panel, HsTheme.line, 12, this));
            chip.setOnClickListener(v -> amountInput.setText(a));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(32), 1);
            lp.rightMargin = dp(8);
            quick.addView(chip, lp);
        }
        body.addView(quick);

        LinearLayout note = new LinearLayout(this);
        note.setOrientation(LinearLayout.VERTICAL);
        note.setPadding(dp(14), dp(10), dp(14), dp(10));
        note.setBackground(HsTheme.round(HsTheme.glow, 14, this));
        note.addView(text("下一步填写收款账号与提现密码", 9, HsTheme.gold, true));
        note.addView(text("提现密码连续错误 5 次将锁定 10 分钟；审核通过后预计 2 小时内到账。", 7, HsTheme.muted, false));
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = dp(12);
        body.addView(note, nlp);

        TextView submit = text("下一步，确认提现", 13, Color.WHITE, true);
        submit.setGravity(Gravity.CENTER);
        submit.setBackground(HsTheme.gradient(14, this));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        slp.topMargin = dp(14);
        submit.setOnClickListener(v -> next());
        body.addView(submit, slp);

        HsNav.add(this, root, 3);
        load();
    }

    private void next() {
        String v = amountInput.getText().toString().trim();
        double amount = 0;
        try { amount = Double.parseDouble(v); } catch (Throwable ignored) {}
        if (amount <= 0) {
            amountInput.setError("请输入提现金额");
            return;
        }
        if (balance > 0 && amount > balance) {
            amountInput.setError("超过可提现余额");
            return;
        }
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(MainActivity.EXTRA_URL, Api.base() + "/withdraw?amount=" + String.format("%.2f", amount));
        startActivity(i);
    }

    private void load() {
        pool.execute(() -> {
            JSONObject s = Api.session();
            if (s == null) return;
            final double bal = s.optDouble("balance", 0);
            ui.post(() -> {
                balance = bal;
                balanceView.setText("¥ " + String.format("%.2f", bal));
            });
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
