package com.hs.club;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 与网页版一致的五栏底部导航：首页 / 活动 / 充值 / 提现 / 我的 */
public final class HsNav {
    private HsNav() {}

    public static void add(Activity activity, LinearLayout root, int active) {
        LinearLayout nav = new LinearLayout(activity);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setBackgroundColor(HsTheme.panel);
        nav.setPadding(HsTheme.dp(activity, 4), HsTheme.dp(activity, 5),
                HsTheme.dp(activity, 4), HsTheme.dp(activity, 7));
        String[] labels = {"首页", "活动", "充值", "提现", "我的"};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView v = new TextView(activity);
            v.setText(labels[i]);
            v.setTextSize(9);
            v.setGravity(Gravity.CENTER);
            v.setTextColor(i == active ? HsTheme.b : HsTheme.muted);
            if (i == active) {
                v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                v.setBackground(HsTheme.round(HsTheme.glow, 12, activity));
            }
            v.setOnClickListener(x -> go(activity, idx));
            nav.addView(v, new LinearLayout.LayoutParams(0, HsTheme.dp(activity, 40), 1));
        }
        root.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, HsTheme.dp(activity, 54)));
    }

    private static void go(Activity activity, int idx) {
        if (idx == 0) {
            Intent i = new Intent(activity, NativeHomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(i);
        } else if (idx == 1) {
            activity.startActivity(new Intent(activity, ActivitiesActivity.class));
        } else if (idx == 2) {
            activity.startActivity(new Intent(activity, RechargeActivity.class));
        } else if (idx == 3) {
            activity.startActivity(new Intent(activity, WithdrawActivity.class));
        } else {
            activity.startActivity(new Intent(activity, ProfileActivity.class));
        }
    }
}
