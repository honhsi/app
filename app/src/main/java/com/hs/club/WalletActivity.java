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
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WalletActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private TextView balanceView;
    private TextView pendingView;
    private ListView listView;
    private OrderAdapter adapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HsTheme.init(this);
        HsTheme.apply(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(HsTheme.bg);
        setContentView(root);

        // 原生顶栏
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(16), dp(12), dp(16), dp(8));
        TextView back = text("‹", 28, HsTheme.a, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(32), dp(40)));
        TextView title = text("我的钱包", 18, HsTheme.text, true);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, dp(40), 1);
        tlp.leftMargin = dp(4);
        bar.addView(title, tlp);
        TextView detail = text("明细 ›", 11, HsTheme.a, false);
        detail.setOnClickListener(v -> open("/report"));
        bar.addView(detail);
        root.addView(bar);

        // 余额卡
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(16), dp(18), dp(16));
        hero.setBackground(HsTheme.gradient(22, this));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.setMargins(dp(16), dp(4), dp(16), 0);
        root.addView(hero, hlp);
        hero.addView(text("可用余额 (CNY)", 10, Color.WHITE, false));
        balanceView = text("¥ 0.00", 30, Color.WHITE, true);
        balanceView.setPadding(0, dp(2), 0, dp(4));
        hero.addView(balanceView);
        LinearLayout pills = new LinearLayout(this);
        pills.setOrientation(LinearLayout.HORIZONTAL);
        pendingView = pill("待打码 ¥0.00");
        TextView withdrawable = pill("可提现 ¥0.00");
        pills.addView(pendingView, new LinearLayout.LayoutParams(0, dp(30), 1));
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(dp(8), dp(30));
        pills.addView(new View(this), gap);
        pills.addView(withdrawable, new LinearLayout.LayoutParams(0, dp(30), 1));
        hero.addView(pills, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));

        // 操作
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        alp.setMargins(dp(16), dp(12), dp(16), 0);
        root.addView(actions, alp);
        TextView dep = action("＋  充值", true);
        TextView wd = action("↑  提现", false);
        actions.addView(dep, new LinearLayout.LayoutParams(0, dp(50), 1));
        LinearLayout.LayoutParams ag = new LinearLayout.LayoutParams(dp(10), dp(50));
        actions.addView(new View(this), ag);
        actions.addView(wd, new LinearLayout.LayoutParams(0, dp(50), 1));
        dep.setOnClickListener(v -> startActivity(new Intent(this, RechargeActivity.class)));
        wd.setOnClickListener(v -> startActivity(new Intent(this, WithdrawActivity.class)));

        // 快捷入口
        LinearLayout quick = new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        quick.setPadding(dp(6), dp(12), dp(6), dp(6));
        quick.setBackground(HsTheme.round(HsTheme.panel, 16, this));
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
        qlp.setMargins(dp(16), dp(12), dp(16), 0);
        root.addView(quick, qlp);
        quick.addView(quickItem("卡", "收款账号", "/cards"), new LinearLayout.LayoutParams(0, dp(64), 1));
        quick.addView(quickItem("记", "提现记录", "/report"), new LinearLayout.LayoutParams(0, dp(64), 1));
        quick.addView(quickItem("安", "安全设置", "/security"), new LinearLayout.LayoutParams(0, dp(64), 1));

        LinearLayout section = new LinearLayout(this);
        section.setGravity(Gravity.CENTER_VERTICAL);
        section.setPadding(dp(18), dp(14), dp(18), dp(6));
        section.addView(text("最近明细", 14, HsTheme.text, true), new LinearLayout.LayoutParams(0, dp(24), 1));
        TextView all = text("全部 ›", 10, HsTheme.a, false);
        all.setOnClickListener(v -> open("/report"));
        section.addView(all);
        root.addView(section);

        listView = new ListView(this);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setPadding(dp(16), 0, dp(16), dp(8));
        listView.setClipToPadding(false);
        listView.setBackgroundColor(Color.TRANSPARENT);
        root.addView(listView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        adapter = new OrderAdapter();
        listView.setAdapter(adapter);

        HsNav.add(this, root, 4);
        load();
    }

    private TextView pill(String label) {
        TextView v = text(label, 9, Color.WHITE, true);
        v.setGravity(Gravity.CENTER);
        v.setBackground(HsTheme.round(Color.parseColor("#8f77e8"), 14, this));
        return v;
    }

    private TextView action(String label, boolean primary) {
        TextView v = text(label, 13, primary ? Color.WHITE : HsTheme.b, true);
        v.setGravity(Gravity.CENTER);
        v.setBackground(primary ? HsTheme.gradient(14, this)
                : HsTheme.roundStroke(HsTheme.panel, HsTheme.b, 14, this));
        return v;
    }

    private View quickItem(String icon, String label, String path) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        TextView ic = text(icon, 10, HsTheme.b, true);
        ic.setGravity(Gravity.CENTER);
        ic.setBackground(HsTheme.round(HsTheme.glow, 12, this));
        box.addView(ic, new LinearLayout.LayoutParams(dp(30), dp(30)));
        TextView tv = text(label, 8, HsTheme.muted, false);
        tv.setPadding(0, dp(4), 0, 0);
        box.addView(tv);
        box.setOnClickListener(v -> open(path));
        return box;
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
            TextView v = text(labels[i], 9, i == 2 ? HsTheme.b : HsTheme.muted, i == 2);
            v.setGravity(Gravity.CENTER);
            if (i == 2) v.setBackground(HsTheme.round(HsTheme.glow, 12, this));
            v.setOnClickListener(x -> {
                if (idx == 0) {
                    Intent i1 = new Intent(this, NativeHomeActivity.class);
                    i1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i1);
                } else if (idx == 1) {
                    startActivity(new Intent(this, ActivitiesActivity.class));
                } else if (idx == 3) {
                    startActivity(new Intent(this, ProfileActivity.class));
                }
            });
            nav.addView(v, new LinearLayout.LayoutParams(0, dp(42), 1));
        }
        root.addView(nav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
    }

    private void load() {
        pool.execute(() -> {
            try {
                JSONObject s = Api.getJson("/appapi/wallet.php");
                final double bal = s.optDouble("balance", 0);
                final double pending = s.optDouble("pending_wager", s.optDouble("bonus_locked", 0));
                final List<Order> rows = new ArrayList<>();
                JSONArray arr = s.optJSONArray("orders");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o == null) continue;
                        Order order = new Order();
                        order.typeName = o.optString("type_name", "");
                        order.orderNo = o.optString("order_no", "");
                        order.amount = o.optDouble("amount", 0);
                        order.statusName = o.optString("status_name", "");
                        order.time = o.optString("create_time", "");
                        rows.add(order);
                    }
                }
                ui.post(() -> {
                    balanceView.setText("¥ " + String.format("%.2f", bal));
                    pendingView.setText("待打码 ¥" + String.format("%.2f", Math.max(0, pending)));
                    adapter.setData(rows);
                });
            } catch (Exception ignored) {}
        });
    }

    private static class Order {
        String typeName, orderNo, statusName, time;
        double amount;
    }

    private class OrderAdapter extends BaseAdapter {
        private final List<Order> data = new ArrayList<>();

        void setData(List<Order> rows) {
            data.clear();
            if (rows != null) data.addAll(rows);
            notifyDataSetChanged();
        }

        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int position) { return data.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            Order o = data.get(position);
            LinearLayout row = new LinearLayout(WalletActivity.this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(14), dp(10), dp(14), dp(10));
            row.setBackground(HsTheme.round(HsTheme.panel, 12, WalletActivity.this));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            row.setLayoutParams(lp);

            LinearLayout top = new LinearLayout(WalletActivity.this);
            top.setGravity(Gravity.CENTER_VERTICAL);
            TextView name = text(o.typeName, 12, HsTheme.text, true);
            top.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView amt = text((o.amount >= 0 ? "+" : "") + String.format("%.2f", o.amount),
                    13, o.amount < 0 ? Color.parseColor("#d4380d") : Color.parseColor("#389e0d"), true);
            top.addView(amt);
            row.addView(top);

            TextView meta = text(o.time + "  ·  " + o.statusName, 9, HsTheme.muted, false);
            meta.setPadding(0, dp(4), 0, 0);
            row.addView(meta);
            return row;
        }
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
