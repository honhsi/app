package com.hs.club;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;

import org.json.JSONObject;

/**
 * 原生界面主题令牌。
 * 与网页 front_theme_palettes() 使用同一组颜色；启动时读取缓存，后台静默刷新。
 */
public final class HsTheme {
    public static int bg = Color.parseColor("#f8f5ff");
    public static int bg2 = Color.parseColor("#eee8ff");
    public static int panel = Color.parseColor("#ffffff");
    public static int panel2 = Color.parseColor("#fbf9ff");
    public static int text = Color.parseColor("#2b2042");
    public static int muted = Color.parseColor("#8a7ba2");
    public static int line = Color.parseColor("#e6dcf5");
    public static int a = Color.parseColor("#b388ff");
    public static int b = Color.parseColor("#7c6cff");
    public static int c = Color.parseColor("#ff8fd8");
    public static int gold = Color.parseColor("#d98a22");
    public static int glow = Color.parseColor("#ece4ff");
    public static boolean light = true;
    public static String version = "lavender-tiles";

    private static final String PREF = "hs_theme";
    private static boolean fetching = false;

    private HsTheme() {}

    public static void init(Context context) {
        try {
            SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            String raw = p.getString("json", "");
            if (!raw.isEmpty()) applyJson(new JSONObject(raw));
        } catch (Throwable ignored) {}
        if (!fetching) {
            fetching = true;
            new Thread(() -> {
                try {
                    JSONObject json = Api.getJson("/appapi/theme.php");
                    if (json.optBoolean("ok", false)) {
                        applyJson(json);
                        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                                .edit().putString("json", json.toString()).apply();
                    }
                } catch (Throwable ignored) {
                } finally {
                    fetching = false;
                }
            }, "hs-theme").start();
        }
    }

    private static void applyJson(JSONObject json) {
        JSONObject colors = json.optJSONObject("colors");
        if (colors == null) return;
        bg = color(colors.optString("bg", ""), bg);
        bg2 = color(colors.optString("bg2", ""), bg2);
        panel = color(colors.optString("panel", ""), panel);
        panel2 = color(colors.optString("panel2", ""), panel2);
        text = color(colors.optString("text", ""), text);
        muted = color(colors.optString("muted", ""), muted);
        line = color(colors.optString("line", ""), line);
        a = color(colors.optString("a", ""), a);
        b = color(colors.optString("b", ""), b);
        c = color(colors.optString("c", ""), c);
        gold = color(colors.optString("gold", ""), gold);
        glow = color(colors.optString("glow", ""), glow);
        light = json.optBoolean("light", isLight(bg));
        version = json.optString("theme_version", version);
    }

    public static void apply(Activity activity) {
        if (activity == null) return;
        try {
            activity.getWindow().setStatusBarColor(bg);
            activity.getWindow().setNavigationBarColor(panel);
            if (Build.VERSION.SDK_INT >= 23) {
                int flags = activity.getWindow().getDecorView().getSystemUiVisibility();
                if (light) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                else flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= 26) {
                    if (light) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    else flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
                activity.getWindow().getDecorView().setSystemUiVisibility(flags);
            }
        } catch (Throwable ignored) {}
    }

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static int color(String value, int fallback) {
        try {
            String s = value == null ? "" : value.trim();
            if (s.startsWith("#")) return Color.parseColor(s);
            if (s.startsWith("rgba") || s.startsWith("rgb")) {
                int l = s.indexOf('('), r = s.indexOf(')');
                if (l >= 0 && r > l) {
                    String[] p = s.substring(l + 1, r).split(",");
                    int rr = Integer.parseInt(p[0].trim());
                    int gg = Integer.parseInt(p[1].trim());
                    int bb = Integer.parseInt(p[2].trim());
                    int aa = p.length > 3 ? Math.round(Float.parseFloat(p[3].trim()) * 255f) : 255;
                    return Color.argb(aa, rr, gg, bb);
                }
            }
        } catch (Throwable ignored) {}
        return fallback;
    }

    public static boolean isLight(int color) {
        double lum = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255d;
        return lum > 0.62;
    }

    public static GradientDrawable round(int fill, float radiusDp, Context context) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(context, radiusDp));
        return d;
    }

    public static GradientDrawable roundStroke(int fill, int stroke, float radiusDp, Context context) {
        GradientDrawable d = round(fill, radiusDp, context);
        d.setStroke(Math.max(1, dp(context, 1)), stroke);
        return d;
    }

    public static GradientDrawable gradient(float radiusDp, Context context) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{a, b});
        d.setCornerRadius(dp(context, radiusDp));
        return d;
    }
}
