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
import android.widget.Button;
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
    private ListView listView;
    private OrderAdapter adapter;

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
        header.setPadding(dp(18), dp(26), dp(18), dp(18));
        GradientDrawable hg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#241733"), Color.parseColor("#4a2f75")});
        header.setBackground(hg);
        TextView title = new TextView(this);
        title.setText("我的钱包");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(title);
        balanceView = new TextView(this);
        balanceView.setText("¥ 0.00");
        balanceView.setTextColor(Color.parseColor("#e6c27a"));
        balanceView.setTextSize(34);
        balanceView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        balanceView.setPadding(0, dp(18), 0, dp(4));
        header.addView(balanceView);
        TextView hint = new TextView(this);
        hint.setText("账户余额");
        hint.setTextColor(Color.parseColor("#c9b8e8"));
        hint.setTextSize(12);
        header.addView(hint);
        root.addView(header);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(dp(16), dp(18), dp(16), dp(10));
        Button dep = button("充值", "#7c6cff", "#ffffff");
        Button wd = button("提现", "#241733", "#e6c27a");
        actions.addView(dep, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(dp(10), dp(48));
        actions.addView(new android.view.View(this), gap);
        actions.addView(wd, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(actions);

        Button profile = button("个人中心", "#ffffff", "#4a3b63");
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        plp.setMargins(dp(16), dp(8), dp(16), 0);
        root.addView(profile, plp);

        TextView note = new TextView(this);
        note.setText("充值/提现使用 App 内安全页面；支付/二维码会按后台白名单跳转浏览器。");
        note.setTextColor(Color.parseColor("#8a7ba2"));
        note.setTextSize(12);
        note.setPadding(dp(18), dp(24), dp(18), 0);
        note.setLineSpacing(0, 1.5f);
        root.addView(note);

        listView = new ListView(this);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setPadding(dp(14), dp(12), dp(14), dp(12));
        adapter = new OrderAdapter();
        listView.setAdapter(adapter);
        root.addView(listView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        dep.setOnClickListener(v -> open("/recharge"));
        wd.setOnClickListener(v -> open("/withdraw"));
        profile.setOnClickListener(v -> open("/user"));
        load();
    }

    private void load() {
        pool.execute(() -> {
            try {
                JSONObject s = Api.getJson("/appapi/wallet.php");
                final double bal = s.optDouble("balance", 0);
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
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(dp(12));
            row.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            row.setLayoutParams(lp);

            LinearLayout top = new LinearLayout(WalletActivity.this);
            top.setGravity(Gravity.CENTER_VERTICAL);
            TextView name = new TextView(WalletActivity.this);
            name.setText(o.typeName);
            name.setTextColor(Color.parseColor("#2b2042"));
            name.setTextSize(14);
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            top.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView amt = new TextView(WalletActivity.this);
            amt.setText((o.amount >= 0 ? "+" : "") + String.format("%.2f", o.amount));
            amt.setTextColor(Color.parseColor(o.amount < 0 ? "#d4380d" : "#389e0d"));
            amt.setTextSize(15);
            amt.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            top.addView(amt);
            row.addView(top);

            TextView meta = new TextView(WalletActivity.this);
            meta.setText(o.time + "  ·  " + o.statusName);
            meta.setTextColor(Color.parseColor("#8a7ba2"));
            meta.setTextSize(11);
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

    private Button button(String label, String bg, String fg) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setTextColor(Color.parseColor(fg));
        b.setAllCaps(false);
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.parseColor(bg));
        d.setCornerRadius(dp(24));
        b.setBackground(d);
        return b;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
