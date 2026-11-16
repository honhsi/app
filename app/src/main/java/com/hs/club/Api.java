package com.hs.club;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.webkit.CookieManager;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

public final class Api {
    private Api() {}

    public static String base() {
        String b = BuildConfig.INITIAL_URL == null ? "" : BuildConfig.INITIAL_URL.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b;
    }

    private static String cookieFor(String url) {
        try {
            CookieManager cm = CookieManager.getInstance();
            String c = cm.getCookie(url);
            return c == null ? "" : c;
        } catch (Throwable e) {
            return "";
        }
    }

    private static void storeCookies(HttpURLConnection conn) {
        try {
            CookieManager cm = CookieManager.getInstance();
            for (Map.Entry<String, java.util.List<String>> e : conn.getHeaderFields().entrySet()) {
                if (e.getKey() == null || !e.getKey().equalsIgnoreCase("Set-Cookie")) continue;
                for (String v : e.getValue()) {
                    String cookie = v;
                    int semi = cookie.indexOf(';');
                    if (semi >= 0) cookie = cookie.substring(0, semi);
                    cm.setCookie(base() + "/", cookie);
                }
            }
            cm.flush();
        } catch (Throwable ignored) {}
    }

    private static HttpURLConnection open(String path, String method) throws Exception {
        String url = path.startsWith("http") ? path : base() + path;
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(15000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "HSApp/1.0 Android");
        conn.setRequestProperty("Accept", "application/json,text/html,*/*");
        String cookie = cookieFor(base() + "/");
        if (!cookie.isEmpty()) conn.setRequestProperty("Cookie", cookie);
        return conn;
    }

    public static String get(String path) throws Exception {
        HttpURLConnection conn = open(path, "GET");
        int code = conn.getResponseCode();
        InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String text = read(is);
        storeCookies(conn);
        conn.disconnect();
        return text;
    }

    public static byte[] getBytes(String path) throws Exception {
        HttpURLConnection conn = open(path, "GET");
        int code = conn.getResponseCode();
        InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        is.close();
        storeCookies(conn);
        conn.disconnect();
        return bos.toByteArray();
    }

    public static JSONObject getJson(String path) throws Exception {
        return new JSONObject(get(path));
    }

    public static JSONObject post(String path, Map<String, String> form) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Iterator<Map.Entry<String, String>> it = form.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, String> e = it.next();
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), "UTF-8")).append('=').append(URLEncoder.encode(e.getValue(), "UTF-8"));
        }
        HttpURLConnection conn = open(path, "POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        byte[] body = sb.toString().getBytes("UTF-8");
        conn.setFixedLengthStreamingMode(body.length);
        OutputStream os = conn.getOutputStream();
        os.write(body);
        os.close();
        int code = conn.getResponseCode();
        InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String text = read(is);
        storeCookies(conn);
        conn.disconnect();
        return new JSONObject(text);
    }

    public static Bitmap getBitmap(String path) {
        try {
            byte[] b = getBytes(path);
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } catch (Throwable e) {
            return null;
        }
    }

    public static JSONObject session() {
        try { return getJson("/appapi/session.php"); } catch (Exception e) { return null; }
    }

    public static void logout() {
        try { get("/logout"); } catch (Exception ignored) {}
        try {
            CookieManager cm = CookieManager.getInstance();
            cm.removeAllCookies(null);
            cm.flush();
        } catch (Throwable ignored) {}
    }

    public static String csrf() {
        try {
            String html = get("/");
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("name=\"csrf-token\"\\s+content=\"([^\"]+)\"").matcher(html);
            if (m.find()) return m.group(1);
            m = java.util.regex.Pattern.compile("HS_CSRF_TOKEN\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(html);
            if (m.find()) return m.group(1);
        } catch (Throwable ignored) {}
        return "";
    }

    public static JSONObject playGame(String vendor, String gameCode, int gameType) {
        try {
            java.util.HashMap<String, String> f = new java.util.HashMap<>();
            f.put("action", "play_game");
            f.put("vendor_code", vendor);
            f.put("game_code", gameCode);
            f.put("game_type", String.valueOf(gameType));
            String csrf = csrf();
            if (!csrf.isEmpty()) f.put("csrf", csrf);
            return post("/index.php", f);
        } catch (Exception e) {
            return null;
        }
    }

    private static String read(InputStream is) throws Exception {
        if (is == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        is.close();
        return bos.toString("UTF-8");
    }
}
